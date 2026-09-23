package org.firstinspires.ftc.teamcode.peregrine.core.utilities;

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

/**
 * <h3>The online optimizer: turns the precomputed value tables on the SD card into a drive command.</h3>
 *
 * <p>The desktop solver writes one <b>value table</b> per target to the SD card. Each table stores, for
 * every 6D state {@code [x, y, h, vx, vy, w]} (field frame), the minimum time to reach the target's
 * handoff region (see TABLE_FORMAT.MD §1-§2). Each loop this class:</p>
 * <ol>
 *     <li>reads the 7 cells of the Kuhn/Freudenthal simplex around the robot's current state and
 *     recovers the gradient of the value function, {@code grad(V)} (TABLE_FORMAT.MD §3-§6, §9);</li>
 *     <li>projects the velocity part of that gradient through the drivetrain model's control matrix
 *     {@code A_u} from MODEL.JSON (TABLE_FORMAT.MD §8) to get the direction in command space that makes
 *     time-to-go fall fastest;</li>
 *     <li>picks the command on the octahedron {@code |fwd| + |strafe| + |turn| = 1} that best follows that
 *     direction once the traction knee (wheel slip, TABLE_FORMAT.MD §8.3) is accounted for.</li>
 * </ol>
 *
 * <p>It also exposes the target list from MANIFEST.JSON and the honing PID gains from MODEL.JSON
 * (TABLE_FORMAT.MD §8.8), which are used by {@code PIDHold} for the final approach.</p>
 */
@Config
public class OptimalityEngine {

    PeregrineOpMode opMode;

    // Root directory of the removable SD card, i.e. where MANIFEST.JSON, MODEL.JSON and TABLES/ live.
    File sdCard;
    ObjectMapper mapper;
    // Parsed /MANIFEST.JSON, see TABLE_FORMAT.MD §2. Grid/encoding fields are read out of this tree on demand.
    JsonNode manifest;
    // Parsed /MODEL.JSON, the fitted drivetrain model, see TABLE_FORMAT.MD §8.
    JsonNode model;

    // Open chunk files, indexed as table[targetIndex][chunkNumber] (TABLE_FORMAT.MD §5 and §9 "Keep chunk files open").
    RandomAccessFile[][] table;

    // Target name for each target index, e.g. targetNames[0] = "score_left".
    public String[] targetNames;
    // Reverse lookup: target name -> target index.
    public HashMap<String, Integer> targets;
    // The 6D goal state [x, y, h, vx, vy, w] of each target, from manifest "targets[].state".
    public double[][] targetStates;
    // Stored element type, one of "u8", "u16", "f16", "f32" (TABLE_FORMAT.MD §6).
    String dtype;

    // Reusable read buffer of exactly elem_bytes bytes, for a single table value.
    byte[] bytes;

    // A_u from MODEL.JSON: 3x3 map from the (saturated) command [fwd, strafe, turn] to body-frame acceleration.
    double[][] AuBasic;
    // Transpose of A_u, used to pull a body-frame acceleration costate back into command space.
    SimpleMatrix AuT;
    // Traction knee from MODEL.JSON control.saturation.knee (TABLE_FORMAT.MD §8.3).
    double knee;

    // Number of fixed-point iterations findU() spends solving for the tilt parameter alpha.
    public static int iterations = 6;
    // Command axes whose weight |c_i| is at or below deadZone * max|c| are switched off entirely, see solve().
    public static double deadZone = 0.3;

    // Pose captured at the start of solve(), reused by the no-arg getGradient(int) overload.
    Pose2D pose;

    // Ring buffer of the last `window` commands, used by boxcar() for smoothing.
    double[][] pastStates;
    public static int window = 4;

    /**
     * Locates the SD card, parses MANIFEST.JSON and MODEL.JSON, validates the encoding, and opens
     * every chunk file of every target. On any failure it writes a telemetry message, requests the
     * opMode to stop, and returns early, leaving the remaining fields null.
     */
    public OptimalityEngine(PeregrineOpMode opMode) {
        this.opMode = opMode;

        mapper = new ObjectMapper();
        // The desktop solver may write NaN / Infinity into the JSON, which strict JSON does not allow.
        mapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());

        sdCard = findSdCard();
        if(sdCard == null) {
            throw new IllegalStateException("No SD Card found.");
        }

        // TABLE_FORMAT.MD §1: both JSON files sit at the root of the card.
        File manifestFile = new File(sdCard, "MANIFEST.JSON");
        File modelFile = new File(sdCard, "MODEL.JSON");
        if(!(manifestFile.exists() && modelFile.exists())) {
            if(!Environment.getExternalStorageState(sdCard).equals(Environment.MEDIA_MOUNTED)) {
                throw new IllegalStateException("SD Card not mounted, did you format it to FAT32?");
            } else {
                throw new IllegalStateException("No tables found on SD Card");
            }
        }

        try {
            manifest = mapper.readTree(manifestFile);
            model = mapper.readTree(modelFile);
        } catch (Exception e) {
            throw new RuntimeException("Exception while reading JSONs from SD Card", e);
        }

        // TABLE_FORMAT.MD §3: all six axes, velocities included, must be field frame so odometry can be
        // fed in directly without rotation.
        if(!Objects.equals(manifest.get("grid").get("frame").asText(), "field")) {
            throw new IllegalStateException("Tables must be generated in field frame");
        }

        targetNames = new String[manifest.get("targets").size()];
        targets = new HashMap<>();
        targetStates = new double[manifest.get("targets").size()][6];
        dtype = manifest.get("encoding").get("dtype").asText();

        // TABLE_FORMAT.MD §6: only these four storage types are defined.
        if(!Objects.equals(dtype, "u8") && !Objects.equals(dtype, "u16") && !Objects.equals(dtype, "f16")  && !Objects.equals(dtype, "f32")) {
            throw new IllegalStateException("SD card uses unsupported datatype \"" + dtype + "\", use u8, u16, f16, or f32 instead");
        }

        // Cross-check elem_bytes against the dtype, per the table in TABLE_FORMAT.MD §6.
        switch (dtype) {
            case "u8" : if(manifest.get("encoding").get("elem_bytes").asInt() != 1) {
                throw new IllegalStateException("SD card table datatype u8 should be 1 byte");
            } break;
            case "u16" : if(manifest.get("encoding").get("elem_bytes").asInt() != 2) {
                throw new IllegalStateException("SD card table datatype u16 should be 2 bytes");
            } break;
            case "f16" : if(manifest.get("encoding").get("elem_bytes").asInt() != 2) {
                throw new IllegalStateException("SD card table datatype f16 should be 2 bytes");
            } break;
            case "f32" : if(manifest.get("encoding").get("elem_bytes").asInt() != 4) {
                throw new IllegalStateException("SD card table datatype f32 should be 4 bytes");
            } break;
        }

        table = new RandomAccessFile[manifest.get("targets").size()][];

        // For each target: record its name/index/goal state and open all of its chunk files up front,
        // since reopening a file every loop costs far more than the read (TABLE_FORMAT.MD §9).
        for (int p = 0; p < manifest.get("targets").size(); p++) {
            // Arrays are indexed by the target's declared "index", not its position in the JSON list.
            int idx = manifest.get("targets").get(p).get("index").asInt();
            table[idx] = new RandomAccessFile[manifest.get("targets").get(p).get("n_chunks").asInt()];
            targetNames[idx] = manifest.get("targets").get(p).get("name").asText();
            targets.put(targetNames[idx], idx);
            for (int i = 0; i < 6; i++) {
                targetStates[idx][i] = manifest.get("targets").get(p).get("state").get(i).asDouble();
            }
            for (int q = 0; q < manifest.get("targets").get(p).get("n_chunks").asInt(); q++) {
                try {
                    // file_pattern is a printf pattern such as "TABLES/T00C%04d.BIN"; q is the chunk number (§5).
                    table[idx][q] = new RandomAccessFile(new File(sdCard, String.format(Locale.US, manifest.get("targets").get(p).get("file_pattern").asText(), q)), "r");
                } catch (Exception e) {
                    throw new IllegalStateException("Could not find table binary on SD card at " +
                            manifest.get("targets").get(p).get("file_pattern").asText(), e);
                }
            }
        }

        bytes = new byte[manifest.get("encoding").get("elem_bytes").asInt()];

        // Load A_u (TABLE_FORMAT.MD §8.2). Only A_u is needed here: the command enters the dynamics
        // solely through A_u, so the other blocks (drag, Coulomb, k) do not affect which command is best.
        AuBasic = new double[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                AuBasic[i][j] = model.get("A_u").get(i).get(j).asDouble();
            }
        }
        AuT = new SimpleMatrix(AuBasic).transpose();

        // TABLE_FORMAT.MD §8.3: required and always positive. It is not validated here.
        knee = model.get("control").get("saturation").get("knee").asDouble();
    }

    /**
     * Computes the minimum-time drive command toward a target from the robot's current state.
     *
     * <p>Why only the velocity part of the gradient matters: along a trajectory
     * {@code dV/dt = dV/dp . v + dV/dv . a}, and the command only changes {@code a} (TABLE_FORMAT.MD §8.2,
     * "u enters as an acceleration and nothing else"). So the best command maximises
     * {@code lambda . a}, where {@code lambda = -dV/dv} is the direction in velocity space that lowers
     * time-to-go fastest. With {@code a = A_u * (g(u) * u)} that becomes maximising {@code c . (g(u) * u)}
     * with {@code c = A_u^T * lambda}.</p>
     *
     * @param target the target index (see {@link #targets})
     * @return {@code [fwd, strafe, turn]}, L1-normalised to 1 (full power), or all zeros if the gradient
     * is flat, for example when every nearby cell is unreachable.
     */
    public double[] solve(int target) {
        if(pastStates == null) pastStates = new double[window][3];
        pose = opMode.localizer.getPose();
        double[] grad = getGradient(target);
        // lambda = -dV/d(vx, vy, w), still in the field frame.
        SimpleMatrix lambdaField = new SimpleMatrix(new double[][]{{-grad[3]}, {-grad[4]}, {-grad[5]}});
        // Rotate by -h to go from field frame to body frame. A_u is body frame (TABLE_FORMAT.MD §8.4 step 3,
        // §8.5 "Frames"). w is the same in either frame, so the third row/column is the identity.
        double cosH = Math.cos(-pose.getHeading(AngleUnit.RADIANS));
        double sinH = Math.sin(-pose.getHeading(AngleUnit.RADIANS));
        SimpleMatrix rotationMatrix = new SimpleMatrix(new double[][]{{cosH, -sinH, 0}, {sinH, cosH, 0}, {0, 0, 1}});
        SimpleMatrix lambda = rotationMatrix.mult(lambdaField);
        // c_i is how much a unit of command axis i (fwd, strafe, turn) reduces time-to-go.
        SimpleMatrix c = AuT.mult(lambda);
        boolean[] live = new boolean[]{true, true, true};
        double cMax = c.elementMaxAbs();
        // Flat gradient: no direction is better than any other, so request nothing (Drive then hands to the PID).
        if (cMax <= 1e-12) return new double[]{0, 0, 0};
        // Dead zone: switch off axes that contribute little, so the command doesn't dither on minor axes.
        for (int i = 0; i < 3; i++){
            if (Math.abs(c.get(i)) <= deadZone*cMax) { live[i] = false; }
        }
        // Solve for the best command, smooth it over the last few loops, then scale back up to full power.
        return normalizeL1(boxcar(findU(c, live)));
    }

    /** Scales a vector so that the sum of absolute values is 1, i.e. onto the octahedron surface. Zero vectors are returned unchanged. */
    double[] normalizeL1(double[] input) {
        double l1 = 0;
        for (double item : input) {
            l1 += Math.abs(item);
        }
        if (l1 == 0) return input;
        double a = 1/l1;
        double[] output = input.clone();
        for (int i = 0; i < output.length; i++) {
            output[i] *= a;
        }
        return output;
    }

    /**
     * Moving-average ("boxcar") filter over the last {@code window} commands. It hides chatter in the
     * command rather than fixing its cause, hence the "dishonest" label. The history starts as zeros, so
     * the first {@code window - 1} outputs are pulled toward zero (normalizeL1 then scales them back up).
     */
    double[] boxcar(double[] input) { // dishonest
        // Shift the history one slot toward index 0, then put the newest command in the last slot.
        for (int i = 0; i < pastStates.length - 1; i++) {
            pastStates[i] = pastStates[i+1].clone();
        }
        pastStates[pastStates.length - 1] = input.clone();
        // Mean of each of the 3 command axes over the whole window.
        double[] sum = pastStates[0].clone();
        for (int i = 0; i < 3; i++) {
            for (int j = 1; j < pastStates.length; j++) {
                sum[i] += pastStates[j][i];
            }
            sum[i] /= pastStates.length;
        }
        return sum;
    }

    /**
     * Picks the command {@code u} on the face of the octahedron {@code sum|u_i| = 1} (live axes only) that
     * maximises {@code c . (g(|u|) * u)}, where {@code g} is the traction gain of TABLE_FORMAT.MD §8.3.
     *
     * <p>Every candidate has the form {@code u_i = alpha * c_i + beta * sign(c_i)}, with
     * {@code beta = (1 - alpha * L) / n} so that {@code sum|u_i| = 1}.
     * <ul>
     *     <li>{@code alpha = 0} spreads the budget evenly across the live axes. This point has the smallest
     *     Euclidean norm on the face, so it suffers the least wheel slip.</li>
     *     <li>Larger {@code alpha} tilts toward the axis with the biggest {@code |c_i|}. Without slip the best
     *     point would be that single vertex.</li>
     * </ul>
     * With slip the optimum sits in between. Its alpha comes from a fixed-point iteration of the
     * stationarity condition, using {@link #elasticity(double)} to measure how quickly slip eats a
     * larger command.</p>
     *
     * <p>Symbols: {@code n} = number of live axes, {@code L = sum|c_i|}, {@code Q = sum c_i^2},
     * {@code p0 = L^2 / n}, {@code W = Q - p0} (how uneven the c_i are; 0 means all equal). For any alpha,
     * {@code |u|^2 = 1/n + W * alpha^2}.</p>
     *
     * <p>If alpha would push some {@code u_i} past zero (a sign flip), that axis is dropped and the
     * problem is solved again on the remaining axes.</p>
     */
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

        // All live |c_i| are (numerically) equal, so the even split is already optimal.
        if (W <= 1e-12 * Q) return assemble(0, c, sigma, L, n, live);

        // alphaMax: the smallest alpha at which some live u_i reaches 0. That happens for axes with
        // |c_i| < L/n, at alpha = 1 / (L - n|c_i|). `drop` is that axis.
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

        // Fixed-point iteration on alpha: get the slip elasticity G at the current |u|, then solve the
        // stationarity quadratic for the alpha that balances extra alignment with c against extra slip.
        for(int i = 0; i < iterations; i++) {
            double r2 = (1d/n) + W*Math.pow(alpha, 2);   // |u|^2 at this alpha
            double x = Math.sqrt(r2) / knee;             // |u| / knee, the "r" of TABLE_FORMAT.MD §8.3
            double G = elasticity(x);

            double disc = Math.pow(G*p0, 2) + (4d/n) * W * (G - 1);
            // No real root: the optimum is on the boundary of this face, so shrink the face.
            if (disc < 0) {
                clamped = true;
                break;
            }
            alpha = (2d/n) / (G*p0 + Math.sqrt(disc));

            // Past the sign-flip limit: this face's interior has no valid optimum.
            if (alpha >= alphaMax) {
                clamped = true;
                break;
            }
        }

        if (clamped){
            // Nothing can be dropped, so fall back to the even split.
            if (drop < 0) return assemble(0, c, sigma, L, n, live);
            // Otherwise switch off the axis that hit zero first and solve on the smaller face.
            boolean[] newLive = live.clone();
            newLive[drop] = false;
            return findU(c, newLive);
        }

        return assemble(alpha, c, sigma, L, n, live);
    }

    /**
     * Returns {@code 1 - t/sinh(t)} with {@code t = 2x}. This equals minus the elasticity of the traction gain
     * {@code tanh(x)/x} (TABLE_FORMAT.MD §8.3), i.e. {@code -d ln(g) / d ln(x)}. It is 0 when slip is
     * negligible and approaches 1 deep past the knee. A Taylor series is used for small t to avoid
     * cancellation in {@code 1 - t/sinh(t)}.
     */
    double elasticity(double x) {
        double t = 2*x;
        if (t < 1e-2) return (Math.pow(t, 2)/6) * (1 - 7 * Math.pow(t, 2) / 60);
        return 1 - t / Math.sinh(t);
    }

    /** Builds {@code u_i = alpha*c_i + beta*sign(c_i)} on the live axes, with {@code beta} chosen so that {@code sum|u_i| = 1}. Dead axes are 0. */
    double[] assemble(double alpha, SimpleMatrix c, SimpleMatrix sigma, double L, double n, boolean[] live) {
        double beta = (1 - alpha * L) / n;
        double[] u = new double[]{0, 0, 0};
        for (int i = 0; i < 3; i++) {
            if(!live[i]) continue;
            u[i] = alpha*c.get(i) + beta*sigma.get(i);
        }
        return u;
    }

    /**
     * Gradient of the value table at the robot's current state. It uses {@link #pose}, which must
     * already have been set by {@link #solve(int)}, plus live velocities from the localizer. Units match
     * the table: cm, rad, cm/s, rad/s, field frame.
     */
     double[] getGradient(int target) {
        return getGradient(target, new double[]{pose.getX(DistanceUnit.CM), pose.getY(DistanceUnit.CM), pose.getHeading(AngleUnit.RADIANS), opMode.localizer.getVelX(DistanceUnit.CM), opMode.localizer.getVelY(DistanceUnit.CM), opMode.localizer.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)});
    }

    /**
     * Recovers {@code grad(V)} at an arbitrary 6D state by Kuhn (Freudenthal) simplex interpolation. This
     * is the same 7-vertex simplex the solver's Bellman backup is written with, so the table is
     * self-consistent under this read (TABLE_FORMAT.MD §9, "The table is solved with the interpolant you
     * read it with").
     *
     * <p>Method: take the grid cell containing the state and sort the axes by fractional offset, largest
     * first. Starting at the cell's lower corner, step +1 along each axis in that order. This visits 7
     * vertices, and each consecutive pair differs along exactly one axis, which gives that axis's partial
     * derivative.</p>
     *
     * @return {@code dV/d[x, y, h, vx, vy, w]} in seconds per unit of each axis.
     */
     double[] getGradient(int target, double[] state) {

        // NOTE: this requests a stop but does not return, so a wrong-length state will still throw below.
        if(state.length != 6) {
            throw new IllegalArgumentException("The state vector must be 6 values in length.");
        }

        // step[k] from TABLE_FORMAT.MD §3.
        double[] stepSize = new double[6];

        // Continuous (fractional) grid coordinate of the state on each axis, c = (value - min) / step.
        double[] stateIdx = new double[6];
        // Lower corner of the containing cell, floor(c).
        int[] flooredIdx = new int[6];
        // Position within the cell, c - floor(c), in [0, 1).
        double[] offsets = new double[6];

        for (int i = 0; i < 6; i++) {
            if (!manifest.get("grid").get("wrap").get(i).asBoolean()) {
                // Normal axis: both endpoints are stored, so there are n-1 gaps (§3 "Sample spacing").
                stepSize[i] = (manifest.get("grid").get("max").get(i).asDouble() - manifest.get("grid").get("min").get(i).asDouble()) / (manifest.get("grid").get("n").get(i).asDouble() - 1);
                stateIdx[i] = (state[i] - manifest.get("grid").get("min").get(i).asDouble()) / stepSize[i];
                // unwrappedClamp is currently a no-op (see below). Out-of-span values are NOT clamped here;
                // readValueIdx treats them as unreachable instead.
                stateIdx[i] = unwrappedClamp(stateIdx[i], i);
                flooredIdx[i] = (int) unwrappedClamp(Math.floor(stateIdx[i]), i);
            } else {
                // Periodic (heading) axis: -pi and +pi are the same sample, so there are n gaps, not n-1 (§3).
                stepSize[i] = 2*Math.PI/manifest.get("grid").get("n").get(i).asDouble();
                stateIdx[i] = (state[i] - manifest.get("grid").get("min").get(i).asDouble()) / stepSize[i];
                // Fold into [0, n) so that index n wraps back to 0 (§3 "mod on the heading axis").
                stateIdx[i] = wrappedClamp(stateIdx[i], i);
                flooredIdx[i] = (int) wrappedClamp(Math.floor(stateIdx[i]), i);
            }
            offsets[i] = stateIdx[i] - flooredIdx[i];
        }

        // Sort the axis indices by offset, descending. This is a selection sort that moves the smallest
        // remaining offset to the end on each pass. The result is the order the Kuhn simplex walk visits.
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

        // The 7 simplex vertices (as 6 per-axis grid indices each) and the value read at each one.
        int[][] points = new int[7][6];
        double[] values = new double[7];

        // Vertex 0 is the cell's lower corner. Vertex i is vertex i-1 moved +1 along axisOrder[i-1].
        points[0] = flooredIdx.clone();
        for(int i = 1; i < 7; i++) {
            points[i] = points[i-1].clone();
            points[i][axisOrder[i-1]]++;
            if(!manifest.get("grid").get("wrap").get(axisOrder[i-1]).asBoolean()) {
                points[i][axisOrder[i-1]] = (int) unwrappedClamp(points[i][axisOrder[i-1]], axisOrder[i-1]);
            } else {
                // Index n-1 and index 0 are neighbours on the heading axis (§3).
                points[i][axisOrder[i-1]] = (int) wrappedClamp(points[i][axisOrder[i-1]], axisOrder[i-1]);
            }
        }

        for (int i = 0; i < 7; i++) {
            values[i] = readValueIdx(target, points[i]);
            // Unreachable-with-no-escape (+infinity, §6) cannot be differenced, so it becomes a large finite
            // penalty (5e7 s). That is far above the escape band, which readers offset by 5e4 s. The resulting
            // steep gradient pushes the robot away from those cells.
            if(!Double.isFinite(values[i])) values[i] = 50000000;
        }

        double[] output = new double[6];

        // Each simplex edge i -> i+1 moves along exactly one axis, so its difference quotient is that axis's partial.
        for (int i = 0; i < 6; i++) {
            output[axisOrder[i]] = (values[i+1] - values[i])/stepSize[axisOrder[i]];
        }

        return output;
    }

    /**
     * Reads and decodes one table value at the given per-axis grid indices (TABLE_FORMAT.MD §4-§6).
     *
     * @param target target index
     * @param state six per-axis indices {@code [ix, iy, ih, ivx, ivy, iw]}. Wrapped axes must already be folded into range.
     * @return time-to-go in seconds; {@code 50000 + escape_seconds} for an escape-band cell;
     * {@code +infinity} for an unreachable cell, an out-of-range index, or a read error.
     */
    double readValueIdx(int target, int[] state) {
        // Out-of-range on a non-wrapped axis means "no value available". This covers both the velocity-envelope
        // case (§3, §9 "Stay inside the velocity envelope") and positions outside the table span. §3
        // says position axes should clamp instead, which this does not do.
        for(int i = 0; i < 6; i++) {
            if((state[i] < 0 || state[i] >= manifest.get("grid").get("n").get(i).asInt()) && !manifest.get("grid").get("wrap").get(i).asBoolean())
                return Double.POSITIVE_INFINITY;
        }

        // TABLE_FORMAT.MD §4: row-major mixed-radix flat index,
        // idx = ((((ix*Ny + iy)*Nh + ih)*Nvx + ivx)*Nvy + ivy)*Nw + iw, computed in long to avoid overflow.
        long idx = (((((long) state[0] * manifest.get("grid").get("n").get(1).asInt() + state[1])
                                       * manifest.get("grid").get("n").get(2).asInt() + state[2])
                                       * manifest.get("grid").get("n").get(3).asInt() + state[3])
                                       * manifest.get("grid").get("n").get(4).asInt() + state[4])
                                       * manifest.get("grid").get("n").get(5).asInt() + state[5];

        // TABLE_FORMAT.MD §5: chunk = idx >> chunk_shift, elem_in_chunk = idx & (chunk_elements - 1),
        // byte_offset = elem_in_chunk * elem_bytes. The shift and mask work because chunk_elements is a power of two.
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

        // TABLE_FORMAT.MD §6: decode according to dtype.
        switch (dtype) {
            case "u8":  return parseUint8(bytes);
            case "u16": return parseUint16(bytes);
            case "f16": return parseFloat16(bytes);
            case "f32": return parseFloat32(bytes);
        }

        opMode.telem.addLine("A value from the pathing table could not be retrieved");
        return Double.POSITIVE_INFINITY;
    }

    /*
     * Decoders (TABLE_FORMAT.MD §6 and §6.1).
     *
     * This DELIBERATELY departs from the spec on the escape band. §6.1 says an escape cell should decode
     * as +infinity and escape_seconds should be a separate second lookup. Here every decoder instead
     * returns a single number:
     *     reachable                 -> time-to-go in seconds (always < value_cap, 60 s by default)
     *     escape band               -> 50000 + escape_seconds
     *     unreachable, no way out   -> +infinity (later replaced by 5e7 in getGradient)
     * The 50000 s offset guarantees any escape cell loses to any real route, while still leaving a
     * gradient inside obstacles that leads toward the nearest reachable cell. A single simplex read
     * therefore handles normal driving and escaping.
     */

    /** u8: 1 byte, raw * scale seconds, with the unreachable sentinel and optional escape band. */
    double parseUint8(byte[] bytes) {
        int raw = bytes[0] & 0xFF;
        if(raw == manifest.get("encoding").get("unreachable").asInt()) return Double.POSITIVE_INFINITY;
        try {
            // escape_seconds = escape_scale * (raw - escape_base)^2  (§6.1, square-law band).
            if (manifest.get("encoding").get("escape_codes").asInt() != 0 && raw >= manifest.get("encoding").get("escape_base").asInt())
                return 50000 + manifest.get("encoding").get("escape_scale").asDouble()
                        * Math.pow(raw - manifest.get("encoding").get("escape_base").asInt(), 2);
        // Cards written before the escape band existed have no escape_* fields, so get() returns null.
        } catch (NullPointerException ignored) {}
        return manifest.get("encoding").get("scale").asDouble() * raw;
    }

    /** u16: 2 bytes little-endian, raw * scale seconds (the default is plain milliseconds), with sentinel and escape band. */
    double parseUint16(byte[] bytes) {
        // Little-endian: bytes[0] is the low byte (§6, "byte_order": "little").
        int raw = ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
        if(raw == manifest.get("encoding").get("unreachable").asInt()) return Double.POSITIVE_INFINITY;
        try {
            // escape_seconds = escape_scale * (raw - escape_base)^2  (§6.1, square-law band).
            if (manifest.get("encoding").get("escape_codes").asDouble() != 0 && raw >= manifest.get("encoding").get("escape_base").asDouble())
                return 50000 + manifest.get("encoding").get("escape_scale").asDouble()
                        * Math.pow(raw - manifest.get("encoding").get("escape_base").asDouble(), 2);
        // Cards written before the escape band existed have no escape_* fields, so get() returns null.
        } catch (NullPointerException ignored) {}
        return manifest.get("encoding").get("scale").asDouble() * raw;
    }

    /**
     * f16: IEEE half precision, little-endian, already in seconds (no scale). NaN and infinity (exponent
     * all ones) are unreachable. A negative value is an escape cell with escape_seconds = -raw (§6.1).
     */
    double parseFloat16(byte[] bytes) {
        int bits = ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
        int sign = (bits >> 15) & 0x1;
        int exponent = (bits >> 10) & 0x1F;
        int mantissa = bits & 0x3FF;

        if (exponent == 0) {
            if (mantissa == 0) {
                return sign == 0 ? 0.0 : -0.0;
            } else {
                // Subnormal half: 2^-14 * (mantissa / 1024).
                double raw = Math.pow(2, -14) * (mantissa / 1024.0);
                return sign == 0 ? raw : 50000 + raw;
            }
        } else if (exponent == 31) {
            // NaN or +/-infinity: unreachable with no way out.
            return Double.POSITIVE_INFINITY;
        }

        // Normal half: (1 + mantissa/1024) * 2^(exponent - 15). The magnitude is used and the sign selects the escape band.
        double raw = (1 + mantissa / 1024.0) * Math.pow(2, exponent - 15);
        return sign == 0 ? raw : 50000 + raw;
    }

    /** f32: IEEE single, little-endian, already in seconds. NaN/infinity are unreachable; a negative value is an escape cell. */
    double parseFloat32(byte[] bytes) {
        int bits = ((bytes[3] & 0xFF) << 24) |
                   ((bytes[2] & 0xFF) << 16) |
                   ((bytes[1] & 0xFF) << 8 ) |
                    (bytes[0] & 0xFF);
        double raw = Float.intBitsToFloat(bits);
        if(!Double.isFinite(raw)) return Double.POSITIVE_INFINITY;
        return raw < 0 ? 50000 - raw : raw;
    }

    /** Closes every open chunk file. Called by PeregrineOpMode when the opMode ends. Safe if the constructor bailed out early. */
    public void closeReaders() {
        if (table == null) return;
        for (RandomAccessFile[] chunks : table) {
            if (chunks == null) continue;
            for (RandomAccessFile chunk : chunks) {
                try {if (chunk != null) chunk.close();} catch (Exception e) {e.printStackTrace();}
            }
        }
    }

    /**
     * Finds the root of the removable SD card. Android only exposes an app-specific directory on it,
     * {@code /storage/XXXX-XXXX/Android/data/<package>/files}, so this walks four levels up from there
     * to reach {@code /storage/XXXX-XXXX}.
     */
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
            throw new RuntimeException("Unexpected error while finding SD Card", e);
        }
        return null;
    }

    // Placeholder for clamping a non-wrapped axis index into [0, n-1]. It is currently the identity, so
    // out-of-span indices reach readValueIdx and read as unreachable. See TABLE_FORMAT.MD §3: position axes
    // are meant to clamp, and velocity axes are meant to report "no value available".
    private double unwrappedClamp(double input, int axis) {
        return input;
    }

    // Folds an index on a periodic axis into [0, n) (TABLE_FORMAT.MD §3, i = mod(round(c), n)).
    private double wrappedClamp(double input, int axis) {
        return mod(input, manifest.get("grid").get("n").get(axis).asDouble());
    }

    // Always-non-negative modulo. Java's % keeps the sign of the dividend.
    private double mod(double a, double b) {
        return ((a % b) + b) % b;
    }

    /** Returns the 6D goal state [x, y, h, vx, vy, w] of the target with the given index, read from the manifest. */
    public double[] getTargetCoords(int target) {
        double[] output = new double[]{0, 0, 0, 0, 0, 0};
        for (int i = 0; i < manifest.get("targets").size(); i++) {
            if (manifest.get("targets").get(i).get("index").asInt() != target) continue;
            for (int j = 0; j < 6; j++) {
                output[j] = manifest.get("targets").get(i).get("state").get(j).asDouble();
            }
            return output;
        }
        throw new NullPointerException("No such target");
    }

    /**
     * Honing PID parameters from MODEL.JSON "honing" (TABLE_FORMAT.MD §8.8), in this order:
     * {@code [Kp, Ki, Kd, B_eff_inv, Lambda, integral_limit]}. The first five are 3x3 and
     * integral_limit is a 3x1 column. All are body frame.
     */
    public SimpleMatrix[] getPIDConstants() {
        SimpleMatrix[] output = new SimpleMatrix[6];
        output[0] = new SimpleMatrix(jsonToDoubleMatrix(model.get("honing").get("Kp")));
        output[1] = new SimpleMatrix(jsonToDoubleMatrix(model.get("honing").get("Ki")));
        output[2] = new SimpleMatrix(jsonToDoubleMatrix(model.get("honing").get("Kd")));
        output[3] = new SimpleMatrix(jsonToDoubleMatrix(model.get("honing").get("B_eff_inv")));
        output[4] = new SimpleMatrix(jsonToDoubleMatrix(model.get("honing").get("Lambda")));
        output[5] = new SimpleMatrix(jsonToDoubleMatrix(model.get("honing").get("integral_limit").get("value")));
        return output;
    }

    // Converts a JSON 2D array to double[][]. A 1D array of numbers becomes an n x 1 column.
    private double[][] jsonToDoubleMatrix(JsonNode jsonMatrix) {
        double[][] output = new double[jsonMatrix.size()][];
        for(int i = 0; i < jsonMatrix.size(); i++) {
            if(!jsonMatrix.get(i).isArray()){
                output[i] = new double[1];
                output[i][0] = jsonMatrix.get(i).asDouble();
                continue;
            }
            output[i] = new double[jsonMatrix.get(i).size()];
            for(int j = 0; j < jsonMatrix.get(i).size(); j++) {
                output[i][j] = jsonMatrix.get(i).get(j).asDouble();
            }
        }
        return output;
    }
}
