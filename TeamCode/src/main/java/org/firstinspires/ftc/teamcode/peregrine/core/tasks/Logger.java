package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import android.content.Context;
import android.os.Environment;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.function.DoubleSupplier;

/**
 * <h3>Logs motor powers and odometry state to a timestamped CSV on the SD card, once per loop.</h3>
 *
 * <p>Columns: {@code timestamp (ms since construction), FR, FL, BR, BL (motor powers), x, y (cm),
 * h (rad), x_vel, y_vel (cm/s), h_vel (rad/s)}. Pose and velocities are field frame, as reported by
 * the Pinpoint. These logs are the input to the desktop drivetrain fit (TABLE_FORMAT.MD §8.6).</p>
 *
 * <p>Never finishes on its own: run() always returns false.</p>
 */
public class Logger extends Task {

    class LogItem {
        String name;
        DoubleSupplier evaluator;

        public LogItem(String name, DoubleSupplier evaluator) {
            this.name = name;
            this.evaluator = evaluator;
        }

        public double evaluate() {
            return evaluator.getAsDouble();
        }
    }

    ArrayList<LogItem> logItems;
    Boolean hasRun;

    PeregrineOpMode opMode;

    // Cleared when the card is missing or a write fails.
    boolean sdInserted = true;
    // NOTE: unlike OptimalityEngine.findSdCard(), this is the app-specific directory on the card
    // (/storage/XXXX-XXXX/Android/data/<package>/files), not the card root, so logs land in <that>/logs.
    File sdCard;
    FileWriter writer;
    File logFile;

    // Measures the timestamp column.
    ElapsedTime time;

    public Logger(PeregrineOpMode opMode) {
        this.opMode = opMode;
        logItems = new ArrayList<>();
        hasRun = false;

        sdCard = findSdCard();
        // Error code "1": no removable storage found.
        if(!sdInserted || sdCard == null) {
            opMode.telem.addLine("1");
            opMode.telem.update();
            opMode.requestOpModeStop();
            return;
        }

        File logDir = new File(sdCard, "logs");
        if(!logDir.exists()) {
            // Error code "2": the logs directory could not be created (usually the card is not mounted).
            if(!logDir.mkdirs()) {
                sdInserted = false;
                opMode.telem.addData("Mounted", Environment.getExternalStorageState(sdCard).equals(Environment.MEDIA_MOUNTED));
                opMode.telem.addLine("2");
                opMode.telem.update();
                opMode.requestOpModeStop();
                return;
            }
        }

        // One new file per construction, e.g. calibration_log_20260910_153000.csv.
        logFile = new File(logDir, "calibration_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv");

        try {
            writer = new FileWriter(logFile, true);
            writer.write("timestamp");
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

    public void addLogItem(String name, DoubleSupplier value) {
        if(hasRun) return; // TODO: make this output an error code
        logItems.add(new LogItem(name, value));

        try {
            writer.write("," + name);
            writer.flush();
        } catch (Exception e) {
            opMode.telem.addData("Exception", e);
            opMode.telem.update();
            sdInserted = false;
            opMode.requestOpModeStop();
            return;
        }
    }

    /** Appends one CSV row with the current motor powers and odometry state. It flushes every row, so a crash loses at most one line. */
    @Override
    public boolean run() {
        hasRun = true;
        try {
            writer.write("\n" + time.milliseconds());
            for (LogItem logItem : logItems) {
                writer.write("," + logItem.evaluate());
            }
            writer.flush();
        } catch (Exception e) {
            opMode.telem.addData("Exception", e);
            opMode.telem.update();
            sdInserted = false;
            opMode.requestOpModeStop();
            return true;
        }
        return false;
    }

    // NOTE: the FileWriter is never closed. Each row is flushed, so no data is lost.
    @Override
    public boolean end() {
        return false;
    }

    // Starts a brand-new log file.
    @Override
    public Task reset() {
        return new Logger(opMode);
    }

    // Returns the app-specific directory on the first removable volume, or null (and clears sdInserted).
    File findSdCard() {
        Context context = AppUtil.getInstance().getActivity();
        try {
            if(context == null) throw new Exception();
            File[] externalDirs = context.getExternalFilesDirs(null);

            for(File dir : externalDirs) {
                if (dir == null) continue;
                if (Environment.isExternalStorageRemovable(dir)) return dir.getParentFile().getParentFile().getParentFile().getParentFile();
            }
        } catch (Exception ignored) {}
        sdInserted = false;
        return null;
    }
}
