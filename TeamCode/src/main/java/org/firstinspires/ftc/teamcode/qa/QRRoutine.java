package org.firstinspires.ftc.teamcode.qa;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class QRRoutine extends LinearOpMode {

    Telemetry telem;
    List<QATest> tests;
    QALogger logger;

    int passCount;
    int totalCount;
    List<QATest> notPassed;

    @Override
    public void runOpMode() {
        if(Objects.equals(QAConfig.logName, "Unnamed Log")) throw new IllegalArgumentException("Rename the log to something else!");
        telem = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        telem.setAutoClear(false);
        tests = tests();
        logger = new QALogger();
        passCount = 0;
        notPassed = new ArrayList<>();
        totalCount = tests.size();

        waitForStart();

        for(QATest test : tests) {
            if (!opModeIsActive()) break;
            String err = "";
            QAContext ctx = new QAContext(this, test);
            try {
                while (!ctx.tick() && opModeIsActive()) {
                    if (ctx.after(test.timeoutMs() / 1000.0)) {
                        test.status = QATest.Status.FAIL;
                        err = "TIMED OUT";
                        break;
                    }
                    telem.update();
                }
            } catch (QAContext.UnavailableSubsystem e) {
                test.status = QATest.Status.SKIP;
                err = e.toString();
            } catch (AssertionError e) {
                test.status = QATest.Status.FAIL;
                err = e.toString();
            } catch (Throwable e) {
                test.status = QATest.Status.ERROR;
                err = e.toString();
            } finally {
                try { ctx.close(); } catch (Throwable ignored) {}
            }
            if (test.status == QATest.Status.RUNNING) {
                test.status = QATest.Status.SKIP;
                err = "STOPPED";
            }
            if(test.status == QATest.Status.PASS) passCount++; else notPassed.add(test);
            telem.clearAll();
            if (!Objects.equals(err, "")) logger.writeToLog(test, err); else logger.writeToLog(test);
        }

        telem.addLine("QR ROUTINE COMPLETE");
        telem.addLine(passCount + "/" + totalCount + " tests passed");
        telem.addLine("Didn't pass:");
        telem.addLine("");

        for(QATest test : notPassed) {
            telem.addLine(test.category + " " + test.name + " " + test.status + " " + test.note);
        }

        telem.update();

        while(opModeIsActive()) {
            sleep(50);
        }

        logger.close();
    }

    public abstract List<QATest> tests();

    public abstract void cleanupHardware(QAContext ctx, boolean hardwareBuilt);
}
