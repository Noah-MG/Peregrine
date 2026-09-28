package org.firstinspires.ftc.teamcode.qa;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <h3>An opMode that runs a list of QA tests and logs the results.</h3>
 *
 * <p>Extend it, add {@code @TeleOp}, and implement {@link #tests()} and
 * {@link #cleanupHardware(QAContext, boolean)}. Before pressing INIT, set QAConfig.tester and
 * QAConfig.logName in FTC Dashboard; the routine refuses to start with the default log name.</p>
 *
 * <p>After START, each test runs in order until it finishes, times out (FAIL), throws, or the opMode
 * is stopped. Every test gets a fresh QAContext, so hardware is rebuilt for each test that uses it. Results are
 * written to the QALogger file as they happen, and a summary of the tests that didn't pass is shown
 * on telemetry at the end. The opMode then waits for STOP.</p>
 */
public abstract class QRRoutine extends LinearOpMode {

    // Driver Station + FTC Dashboard telemetry, shared with every QAContext.
    Telemetry telem;
    List<QATest> tests;
    QALogger logger;

    // Summary counts shown at the end.
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

        // Run each test until it finishes, times out or throws; then clean up and log it.
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
            // Still running here means the opMode was stopped mid-test.
            if (test.status == QATest.Status.RUNNING) {
                test.status = QATest.Status.SKIP;
                err = "STOPPED";
            }
            if(test.status == QATest.Status.PASS) passCount++; else notPassed.add(test);
            telem.clearAll();
            if (!Objects.equals(err, "")) logger.writeToLog(test, err); else logger.writeToLog(test);
        }

        // Summary: how many passed, and the name, status and note of every test that didn't.
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

    /**
     * Builds the tests to run, in order. Called once during INIT.
     * @return the tests, run first to last
     */
    public abstract List<QATest> tests();

    /**
     * Puts the robot in a safe state after each test, for example by turning every motor off. Called
     * after the test's own cleanup, however the test finished.
     *
     * @param ctx the finished test's context
     * @param hardwareBuilt whether the test built the hardware; if false, don't call ctx.hardware()
     */
    public abstract void cleanupHardware(QAContext ctx, boolean hardwareBuilt);
}
