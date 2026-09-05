package org.firstinspires.ftc.teamcode.peregrine.core.utilities;

import static androidx.core.math.MathUtils.clamp;

import android.content.Context;
import android.os.Environment;

import com.acmerobotics.dashboard.config.Config;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.ejml.simple.SimpleMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

@Config
public class OptimalityEngine {

    PeregrineOpMode opMode;

    File sdCard;
    ObjectMapper mapper;
    JsonNode manifest;
    JsonNode model;

    RandomAccessFile[][] table;

    public String[] targetNames;
    public HashMap<String, Integer> targets;
    public double[][] targetStates;
    String dtype;

    byte[] bytes;

    double[][] AuBasic;
    SimpleMatrix AuT;
    double knee;

    public static int iterations = 3;
    public static double deadZone = 0.25;

    Pose2D pose;

    double[][] pastStates;
    public static int window = 4;

    public OptimalityEngine(PeregrineOpMode opMode) {
        this.opMode = opMode;

        mapper = new ObjectMapper();
        mapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());

        sdCard = findSdCard();
        if(sdCard == null) {
            opMode.telem.addLine("No SD Card found");
            opMode.telem.update();
            opMode.requestOpModeStop();
            return;
        }

        File manifestFile = new File(sdCard, "MANIFEST.JSON");
        File modelFile = new File(sdCard, "MODEL.JSON");
        if(!(manifestFile.exists() && modelFile.exists())) {
            opMode.telem.addData("Tables not found on ", sdCard);
            if(!Environment.getExternalStorageState(sdCard).equals(Environment.MEDIA_MOUNTED)) {
                opMode.telem.addLine("SD Card not mounted, did you format it to FAT32?");
            }
            opMode.telem.update();
            opMode.requestOpModeStop();
            return;
        }

        try {
            manifest = mapper.readTree(manifestFile);
            model = mapper.readTree(modelFile);
        } catch (Throwable t) {
            opMode.telem.addData("Error", t);
            opMode.telem.update();
            opMode.requestOpModeStop();
            return;
        }

        if(!Objects.equals(manifest.get("grid").get("frame").asText(), "field")) {
            opMode.telem.addLine("Please make sure that the SD card tables are generated in the field frame");
            opMode.telem.update();
            opMode.requestOpModeStop();
            return;
        }

        targetNames = new String[manifest.get("targets").size()];
        targets = new HashMap<>();
        targetStates = new double[manifest.get("targets").size()][6];
        dtype = manifest.get("encoding").get("dtype").asText();

        if(!Objects.equals(dtype, "u8") && !Objects.equals(dtype, "u16") && !Objects.equals(dtype, "f16")  && !Objects.equals(dtype, "f32")) {
            opMode.telem.addLine("SD card table uses unsupported datatype \"" + dtype + "\", please create table using u8, u16, f16, or f32");
            opMode.telem.update();
            opMode.requestOpModeStop();
            return;
        }

        switch (dtype) {
            case "u8" : if(manifest.get("encoding").get("elem_bytes").asInt() != 1) {
                opMode.telem.addLine("SD card table data type u8 should be 1 byte, please reprocess table");
                opMode.telem.update();
                opMode.requestOpModeStop();
                return;
            } break;
            case "u16" : if(manifest.get("encoding").get("elem_bytes").asInt() != 2) {
                opMode.telem.addLine("SD card table data type u16 should be 2 bytes, please reprocess table");
                opMode.telem.update();
                opMode.requestOpModeStop();
                return;
            } break;
            case "f16" : if(manifest.get("encoding").get("elem_bytes").asInt() != 2) {
                opMode.telem.addLine("SD card table data type f16 should be 2 byte, please reprocess table");
                opMode.telem.update();
                opMode.requestOpModeStop();
                return;
            } break;
            case "f32" : if(manifest.get("encoding").get("elem_bytes").asInt() != 4) {
                opMode.telem.addLine("SD card table data type f32 should be 4 byte, please reprocess table");
                opMode.telem.update();
                opMode.requestOpModeStop();
                return;
            } break;
        }

        table = new RandomAccessFile[manifest.get("targets").size()][];

        for (int p = 0; p < manifest.get("targets").size(); p++) {
            int idx = manifest.get("targets").get(p).get("index").asInt();
            table[idx] = new RandomAccessFile[manifest.get("targets").get(p).get("n_chunks").asInt()];
            targetNames[idx] = manifest.get("targets").get(p).get("name").asText();
            targets.put(targetNames[idx], idx);
            for (int i = 0; i < 6; i++) {
                targetStates[idx][i] = manifest.get("targets").get(p).get("state").get(i).asDouble();
            }
            for (int q = 0; q < manifest.get("targets").get(p).get("n_chunks").asInt(); q++) {
                try {
                    table[idx][q] = new RandomAccessFile(new File(sdCard, String.format(Locale.US, manifest.get("targets").get(p).get("file_pattern").asText(), q)), "r");
                } catch (Exception e) {
                    opMode.telem.addLine("Could not find table binary on SD card at " + manifest.get("targets").get(p).get("file_pattern").asText());
                    opMode.telem.update();
                    opMode.requestOpModeStop();
                    return;
                }
            }
        }

        bytes = new byte[manifest.get("encoding").get("elem_bytes").asInt()];

        AuBasic = new double[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                AuBasic[i][j] = model.get("A_u").get(i).get(j).asDouble();
            }
        }
        AuT = new SimpleMatrix(AuBasic).transpose();

        knee = model.get("control").get("saturation").get("knee").asDouble();
    }

    public double[] solve(int target) {
        if(pastStates == null) pastStates = new double[window][3];
        pose = opMode.localizer.getPose();
        double[] grad = getGradient(target);
        SimpleMatrix lambdaField = new SimpleMatrix(new double[][]{{-grad[3]}, {-grad[4]}, {-grad[5]}});
        double cosH = Math.cos(-pose.getHeading(AngleUnit.RADIANS));
        double sinH = Math.sin(-pose.getHeading(AngleUnit.RADIANS));
        SimpleMatrix rotationMatrix = new SimpleMatrix(new double[][]{{cosH, -sinH, 0}, {sinH, cosH, 0}, {0, 0, 1}});
        SimpleMatrix lambda = rotationMatrix.mult(lambdaField);
        SimpleMatrix c = AuT.mult(lambda);
        boolean[] live = new boolean[]{true, true, true};
        double cMax = c.elementMaxAbs();
        if (cMax <= 1e-12) return new double[]{0, 0, 0};
        for (int i = 0; i < 3; i++){
            if (Math.abs(c.get(i)) <= deadZone*cMax) { live[i] = false; }
        }
        return normalizeL1(boxcar(findU(c, live)));
    }

    double[] normalizeL1(double[] input) {
        double l1 = 0;
        for (double item : input) {
            l1 += Math.abs(item);
        }
        double a = 1/l1;
        double[] output = input.clone();
        for (int i = 0; i < output.length; i++) {
            output[i] *= a;
        }
        return output;
    }

    double[] boxcar(double[] input) { // dishonest
        for (int i = 0; i < pastStates.length - 1; i++) {
            pastStates[i] = pastStates[i+1].clone();
        }
        pastStates[pastStates.length - 1] = input.clone();
        double[] sum = pastStates[0].clone();
        for (int i = 1; i < 3; i++) {
            for (int j = 1; j < pastStates.length; j++) {
                sum[i] += pastStates[j][i];
            }
            sum[i] /= pastStates.length;
        }
        return sum;
    }

    double[] findU(SimpleMatrix c, boolean[] live) {
        int n = 0;
        for (boolean axis : live) {
            if (axis) {n++;}
        }

        SimpleMatrix sigma = new SimpleMatrix(3, 1);
        double L = 0;
        double Q = 0;
        for (int i = 0; i < 3; i++) {
            if(!live[i]) continue;
            sigma.set(i, Math.signum(c.get(i)));
            L += Math.abs(c.get(i));
            Q += Math.pow(c.get(i), 2);
        }
        double p0 = Math.pow(L, 2)/n;
        double W = Q - p0;

        if (W <= 1e-12 * Q) return assemble(0, c, sigma, L, n, live);

        double alphaMax = Double.POSITIVE_INFINITY;
        int drop = -1;
        for (int i = 0; i < 3; i++) {
            if(!live[i]) continue;
            double den = L - n * Math.abs(c.get(i));
            if (den > 0 && 1/den < alphaMax) {
                alphaMax = 1/den;
                drop = i;
            }
        }

        double alpha = Math.min(1/Math.pow(L, 2), alphaMax);
        boolean clamped = false;

        for(int i = 0; i < iterations; i++) {
            double r2 = (1d/n) + W*Math.pow(alpha, 2);
            double x = Math.sqrt(r2) / knee;
            double G = elasticity(x);

            double disc = Math.pow(G*p0, 2) + (4d/n) * W * (G - 1);
            if (disc < 0) {
                clamped = true;
                break;
            }
            alpha = (2d/n) / (G*p0 + Math.sqrt(disc));

            if (alpha >= alphaMax) {
                clamped = true;
                break;
            }
        }

        if (clamped){
            if (drop < 0) return assemble(0, c, sigma, L, n, live);
            boolean[] newLive = live.clone();
            newLive[drop] = false;
            return findU(c, newLive);
        }

        return assemble(alpha, c, sigma, L, n, live);
    }

    double elasticity(double x) {
        double t = 2*x;
        if (t < 1e-2) return (Math.pow(t, 2)/6) * (1 - 7 * Math.pow(t, 2) / 60);
        return 1 - t / Math.sinh(t);
    }

    double[] assemble(double alpha, SimpleMatrix c, SimpleMatrix sigma, double L, double n, boolean[] live) {
        double beta = (1 - alpha * L) / n;
        double[] u = new double[]{0, 0, 0};
        for (int i = 0; i < 3; i++) {
            if(!live[i]) continue;
            u[i] = alpha*c.get(i) + beta*sigma.get(i);
        }
        return u;
    }

     double[] getGradient(int target) {
        return getGradient(target, new double[]{pose.getX(DistanceUnit.CM), pose.getY(DistanceUnit.CM), pose.getHeading(AngleUnit.RADIANS), opMode.localizer.getVelX(DistanceUnit.CM), opMode.localizer.getVelY(DistanceUnit.CM), opMode.localizer.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)});
    }

     double[] getGradient(int target, double[] state) {

        if(state.length != 6) {
            opMode.telem.addLine("The state vector must be 6 values in length!");
            opMode.telem.update();
            opMode.requestOpModeStop();
        }

        double[] stepSize = new double[6];

        double[] stateIdx = new double[6];
        int[] flooredIdx = new int[6];
        double[] offsets = new double[6];

        for (int i = 0; i < 6; i++) {
            if (!manifest.get("grid").get("wrap").get(i).asBoolean()) {
                stepSize[i] = (manifest.get("grid").get("max").get(i).asDouble() - manifest.get("grid").get("min").get(i).asDouble()) / (manifest.get("grid").get("n").get(i).asDouble() - 1);
                stateIdx[i] = (state[i] - manifest.get("grid").get("min").get(i).asDouble()) / stepSize[i];
                stateIdx[i] = unwrappedClamp(stateIdx[i], i);
                flooredIdx[i] = (int) unwrappedClamp(Math.floor(stateIdx[i]), i);
            } else {
                stepSize[i] = 2*Math.PI/manifest.get("grid").get("n").get(i).asDouble();
                stateIdx[i] = (state[i] - manifest.get("grid").get("min").get(i).asDouble()) / stepSize[i];
                stateIdx[i] = wrappedClamp(stateIdx[i], i);
                flooredIdx[i] = (int) wrappedClamp(Math.floor(stateIdx[i]), i);
            }
            offsets[i] = stateIdx[i] - flooredIdx[i];
        }

        int[] axisOrder = new int[]{0, 1, 2, 3, 4, 5};

        for(int x = 0; x < 5; x++) {
            int smallestIndex = 0;
            for(int y = 1; y < 6 - x; y++) {
                smallestIndex = offsets[axisOrder[smallestIndex]] > offsets[axisOrder[y]] ? y : smallestIndex;
            }
            if(smallestIndex != 5-x) {
                int temp = axisOrder[smallestIndex];
                axisOrder[smallestIndex] = axisOrder[5-x];
                axisOrder[5-x] = temp;
            }
        }

        int[][] points = new int[7][6];
        double[] values = new double[7];

        points[0] = flooredIdx.clone();
        for(int i = 1; i < 7; i++) {
            points[i] = points[i-1].clone();
            points[i][axisOrder[i-1]]++;
            if(!manifest.get("grid").get("wrap").get(axisOrder[i-1]).asBoolean()) {
                points[i][axisOrder[i-1]] = (int) unwrappedClamp(points[i][axisOrder[i-1]], axisOrder[i-1]);
            } else {
                points[i][axisOrder[i-1]] = (int) wrappedClamp(points[i][axisOrder[i-1]], axisOrder[i-1]);
            }
        }

        for (int i = 0; i < 7; i++) {
            values[i] = readValueIdx(target, points[i]);
            if(!Double.isFinite(values[i])) values[i] = 500;
        }

        double[] output = new double[6];

        for (int i = 0; i < 6; i++) {
            if(points[i][axisOrder[i]] == points[i+1][axisOrder[i]]) {
                double sign = 5000000 * Math.signum(manifest.get("grid").get("max").get(axisOrder[i]).asInt() - manifest.get("grid").get("min").get(axisOrder[i]).asInt());
                output[axisOrder[i]] = points[i][axisOrder[i]] == manifest.get("grid").get("n").get(axisOrder[i]).asInt() - 1 ? sign : -sign;
                continue;
            }
            output[axisOrder[i]] = (values[i+1] - values[i])/stepSize[axisOrder[i]];
        }

        return output;
    }

    double readValueIdx(int target, int[] state) {
        long idx = (((((long) state[0] * manifest.get("grid").get("n").get(1).asInt() + state[1])
                                       * manifest.get("grid").get("n").get(2).asInt() + state[2])
                                       * manifest.get("grid").get("n").get(3).asInt() + state[3])
                                       * manifest.get("grid").get("n").get(4).asInt() + state[4])
                                       * manifest.get("grid").get("n").get(5).asInt() + state[5];

        int chunk = Math.toIntExact(idx >> manifest.get("encoding").get("chunk_shift").asInt());
        int offset = Math.toIntExact((idx & (manifest.get("encoding").get("chunk_elements").asInt() - 1))
                                           * manifest.get("encoding").get("elem_bytes").asInt());

        try {
            table[target][chunk].seek(offset);
            table[target][chunk].readFully(bytes);
        } catch (Exception e) {
            opMode.telem.addLine("A value from the pathing table could not be retrieved");
            return Double.POSITIVE_INFINITY;
        }

        switch (dtype) {
            case "u8":  return manifest.get("encoding").get("scale").asDouble() * parseUint8(bytes);
            case "u16": return manifest.get("encoding").get("scale").asDouble() * parseUint16(bytes);
            case "f16": return parseFloat16(bytes);
            case "f32": return parseFloat32(bytes);
        }

        opMode.telem.addLine("A value from the pathing table could not be retrieved");
        return Double.POSITIVE_INFINITY;
    }

    double parseUint8(byte[] bytes) {
        int value = bytes[0] & 0xFF;
        return value == manifest.get("encoding").get("unreachable").asInt() ? Double.POSITIVE_INFINITY : value;
    }

    double parseUint16(byte[] bytes) {
        int value = ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
        return value == manifest.get("encoding").get("unreachable").asInt() ? Double.POSITIVE_INFINITY : value;
    }

    double parseFloat16(byte[] bytes) {
        int bits = ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
        int sign = (bits >> 15) & 0x1;
        int exponent = (bits >> 10) & 0x1F;
        int mantissa = bits & 0x3FF;

        if (exponent == 0) {
            if (mantissa == 0) {
                return sign == 0 ? 0.0 : -0.0;
            } else {
                return Math.copySign(Math.pow(2, -14) * (mantissa / 1024.0), sign == 0 ? 1 : -1);
            }
        } else if (exponent == 31) {
            return Double.POSITIVE_INFINITY;
        }

        double value = (1 + mantissa / 1024.0) * Math.pow(2, exponent - 15);
        return sign == 0 ? value : -value;
    }

    double parseFloat32(byte[] bytes) {
        int bits = ((bytes[3] & 0xFF) << 24) |
                   ((bytes[2] & 0xFF) << 16) |
                   ((bytes[1] & 0xFF) << 8 ) |
                    (bytes[0] & 0xFF);
        double value = Float.intBitsToFloat(bits);
        return Double.isNaN(value) ? Double.POSITIVE_INFINITY : value;
    }

    public void closeReaders() {
        if (table == null) return;
        for (RandomAccessFile[] chunks : table) {
            if (chunks == null) continue;
            for (RandomAccessFile chunk : chunks) {
                try {if (chunk != null) chunk.close();} catch (Exception e) {e.printStackTrace();}
            }
        }
    }

    File findSdCard() {
        Context context = AppUtil.getInstance().getActivity();
        try {
            if(context == null) throw new Exception();
            File[] externalDirs = context.getExternalFilesDirs(null);

            for(File dir : externalDirs) {
                if (dir == null) continue;
                if (Environment.isExternalStorageRemovable(dir)) return dir.getParentFile().getParentFile().getParentFile().getParentFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private double unwrappedClamp(double input, int axis) {
        return clamp(input, 0, manifest.get("grid").get("n").get(axis).asDouble() - 1);
    }

    private double wrappedClamp(double input, int axis) {
        return mod(input, manifest.get("grid").get("n").get(axis).asDouble());
    }

    private double mod(double a, double b) {
        return ((a % b) + b) % b;
    }
}