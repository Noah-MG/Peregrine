package org.firstinspires.ftc.teamcode.qa;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.FileWriter;

/**
 * <h3>Writes QA results to a CSV file.</h3>
 *
 * <p>The file is {@code FIRST/qa/<logName>.csv} on the Control Hub's internal storage
 * ({@code /sdcard/FIRST/qa/}), named by QAConfig.logName. If the file already exists, the new run is
 * added to the end of it. Each run starts with a line naming the log and the tester, then a
 * {@code Name,Category,Status,Note} header, then one row per test.</p>
 */
public class QALogger {

    File dir;
    FileWriter writer;
    File logFile;

    /**
     * Opens (or creates) the log file and writes the run's header lines.
     *
     * @throws RuntimeException if the file can't be written.
     */
    public QALogger() {
        dir = new File(AppUtil.ROOT_FOLDER, "qa");
        dir.mkdirs();
        logFile = new File(dir, QAConfig.logName + ".csv");

        try {
            writer = new FileWriter(logFile, true);
            writer.write(QAConfig.logName + " testing by " + QAConfig.tester + "\n");
            writer.write("Name,Category,Status,Note\n");
            writer.flush();
        } catch (Exception e) {
            try { writer.close(); } catch (Exception ignored) {}
            throw new RuntimeException("Unexpected error while writing qa log", e);
        }
    }

    /** Writes one row with the test's name, category, status and note. */
    public void writeToLog(QATest test) {
        try {
            if (test.note == null) {
                writer.write(test.name + "," + test.category + "," + test.status + "\n");
            } else {
                writer.write(test.name + "," + test.category + "," + test.status + "," + test.note + "\n");
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while writing qa log", e);
        }
    }

    /**
     * Writes one row like {@link #writeToLog(QATest)}, with an extra note added after the test's own,
     * such as "TIMED OUT" or an exception message.
     */
    public void writeToLog(QATest test, String note) {
        test.note += "," + note;
        try {
            writer.write(test.name + "," + test.category + "," + test.status + "," + test.note + "\n");
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while writing qa log", e);
        }
    }

    /** Closes the file. */
    public void close() {
        try { writer.close(); } catch (Throwable ignored) {}
    }
}
