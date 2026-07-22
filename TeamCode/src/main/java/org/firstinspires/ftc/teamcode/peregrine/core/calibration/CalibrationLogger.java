package org.firstinspires.ftc.teamcode.peregrine.core.calibration;

import android.content.Context;
import android.os.Environment;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalibrationLogger extends Task {

    PeregrineOpMode opMode;

    boolean sdInserted = true;
    File sdCard;
    FileWriter writer;
    File logFile;

    ElapsedTime time;

    public CalibrationLogger(PeregrineOpMode opMode) {
        this.opMode = opMode;

        sdCard = findSdCard();
        if(!sdInserted || sdCard == null) {
            opMode.requestOpModeStop();
            return;
        }

        File logDir = new File(sdCard, "logs");
        if(!logDir.exists()) {
            if(!logDir.mkdirs()) {
                sdInserted = false;
                opMode.requestOpModeStop();
                return;
            }
        }

        logFile = new File(logDir, "calibration_log_" + new SimpleDateFormat("yyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv");

        try {
            writer = new FileWriter(logFile, true);
            writer.write("timestamp,FR,FL,BR,BL,x,y,h,x_vel,y_vel,h_vel\n");
            writer.flush();
        } catch (Exception e) {
            opMode.telem.addData("Exception", e);
            opMode.telem.update();
            sdInserted = false;
            opMode.requestOpModeStop();
            return;
        }

        time = new ElapsedTime();
    }

    @Override
    public boolean run() {
        double FR = opMode.hardware.FR.getPower();
        double FL = opMode.hardware.FL.getPower();
        double BR = opMode.hardware.BR.getPower();
        double BL = opMode.hardware.BL.getPower();

        Pose2D pose = opMode.localizer.getPose();
        double x = pose.getX(DistanceUnit.CM);
        double y = pose.getY(DistanceUnit.CM);
        double h = pose.getHeading(AngleUnit.RADIANS);
        double x_vel = opMode.localizer.getVelX(DistanceUnit.CM);
        double y_vel = opMode.localizer.getVelY(DistanceUnit.CM);
        double h_vel = opMode.localizer.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS);

        try{
            writer.write(time.milliseconds() + "," + FR + "," + FL + "," + BR + "," + BL + "," + x + "," + y + "," + h + "," + x_vel + "," + y_vel + "," + h_vel);
            writer.flush();
        } catch (Exception e) {
            opMode.telem.addData("Exception", e);
            opMode.telem.update();
            sdInserted = false;
            opMode.requestOpModeStop();
        }
        return false;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return new CalibrationLogger(opMode);
    }

    File findSdCard() {
        Context context = AppUtil.getInstance().getActivity();
        try {
            if(context == null) throw new Exception();
            File[] externalDirs = context.getExternalFilesDirs(null);

            for(File dir : externalDirs) {
                if (dir == null) continue;
                if (Environment.isExternalStorageRemovable(dir)) return dir;
            }
        } catch (Exception ignored) {}
        sdInserted = false;
        return null;
    }
}
