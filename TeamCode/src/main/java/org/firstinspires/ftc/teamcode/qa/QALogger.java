package org.firstinspires.ftc.teamcode.qa;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.FileWriter;

public class QALogger {

    File dir;
    FileWriter writer;
    File logFile;

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

    public void writeToLog(QATest test, String note) {
        test.note += "," + note;
        try {
            writer.write(test.name + "," + test.category + "," + test.status + "," + test.note + "\n");
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while writing qa log", e);
        }
    }

    public void close() {
        try { writer.close(); } catch (Throwable ignored) {}
    }
}
