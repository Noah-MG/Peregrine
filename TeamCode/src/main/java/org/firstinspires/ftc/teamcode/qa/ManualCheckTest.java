package org.firstinspires.ftc.teamcode.qa;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * <h3>A test that a person grades by watching the robot.</h3>
 *
 * <p>The instruction is shown on telemetry, and your step code runs every loop, for example to spin
 * a mechanism so the tester can watch it. The tester then grades it on gamepad 1: A for PASS, B for
 * FAIL, X for SKIP, Y for ERROR. There is no timeout.</p>
 * <pre>{@code
 * new ManualCheckTest("Intake spins inward", QATest.Category.HARDWARE,
 *         ctx -> "",
 *         ctx -> ctx.hardware().intake.setPower(0.5),
 *         "Check the intake is pulling inward")
 * }</pre>
 */
public class ManualCheckTest extends QATest{

    /** Builds the note written to the log once the test finishes. */
    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    /** Runs once per loop until the tester grades the test. */
    public interface StepSupplier {
        void run(QAContext ctx);
    }

    StepSupplier stepSupplier;
    NoteSupplier noteSupplier;
    String instruction;
    boolean firstCall;
    Gamepad gamepad1;

    /**
     * @param name the name shown on telemetry and written to the log
     * @param category what kind of thing the test checks
     * @param noteSupplier builds the log note; must not be null
     * @param stepSupplier runs once per loop until the tester grades the test
     * @param instruction tells the tester what to look for
     */
    public ManualCheckTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, String instruction) {
        super(name, category);
        this.stepSupplier = stepSupplier;
        this.noteSupplier = noteSupplier;
        this.instruction = instruction;
        firstCall = true;
    }

    @Override
    public boolean step(QAContext ctx) {
        if(firstCall) {
            status = Status.RUNNING;
            gamepad1 = ctx.gamepad1();
            firstCall = false;
        }
        ctx.prompt(instruction + "\n Press A for PASS, B for FAIL, X for SKIP, Y for ERROR");
        stepSupplier.run(ctx);

        if(gamepad1.aWasPressed()) status = Status.PASS;
        if(gamepad1.bWasPressed()) status = Status.FAIL;
        if(gamepad1.xWasPressed()) status = Status.SKIP;
        if(gamepad1.yWasPressed()) status = Status.ERROR;
        if(status != Status.RUNNING) {
            note = noteSupplier.get(ctx);
            gamepad1.rumble(200);
            return true;
        }
        return false;
    }

    // Waits for the tester as long as it takes.
    @Override
    protected long timeoutMs() {
        return Long.MAX_VALUE;
    }
}
