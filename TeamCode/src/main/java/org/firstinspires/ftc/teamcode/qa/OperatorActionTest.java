package org.firstinspires.ftc.teamcode.qa;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * <h3>A test that asks a person to do something, and passes when the code sees it happen.</h3>
 *
 * <p>The instruction is shown on telemetry and your step code runs every loop. The test passes as
 * soon as the step returns true, and fails if that hasn't happened within the time limit. The tester
 * can also press X on gamepad 1 for SKIP or Y for ERROR.</p>
 * <pre>{@code
 * new OperatorActionTest("Limit switch", QATest.Category.HARDWARE,
 *         ctx -> "",
 *         ctx -> ctx.hardware().limitSwitch.isPressed(),
 *         "Press the lift's limit switch",
 *         10000)
 * }</pre>
 */
public class OperatorActionTest extends QATest {

    /** Builds the note written to the log once the test finishes. */
    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    /** Runs once per loop and returns true once the tester's action has been detected. */
    public interface StepSupplier {
        boolean run(QAContext ctx);
    }

    NoteSupplier noteSupplier;
    StepSupplier stepSupplier;
    // How long the tester has to do the action, in milliseconds.
    long timeoutMs;
    boolean firstCall;
    // Started on the first step.
    ElapsedTime timer;
    String instruction;
    Gamepad gamepad1;

    /**
     * @param name the name shown on telemetry and written to the log
     * @param category what kind of thing the test checks
     * @param noteSupplier builds the log note; must not be null
     * @param stepSupplier runs once per loop; returns true once the action has been detected
     * @param instruction tells the tester what to do
     * @param timeoutMs how long the tester has before the test fails, in milliseconds
     */
    public OperatorActionTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, String instruction, long timeoutMs) {
        super(name, category);
        this.noteSupplier = noteSupplier;
        this.stepSupplier = stepSupplier;
        this.instruction = instruction;
        this.timeoutMs = timeoutMs;
        firstCall = true;
    }

    @Override
    public boolean step(QAContext ctx) {
        if(firstCall) {
            timer = new ElapsedTime();
            status = Status.RUNNING;
            gamepad1 = ctx.gamepad1();
            firstCall = false;
        }
        ctx.prompt(instruction + "\n Press X for SKIP, Y for ERROR");
        if(stepSupplier.run(ctx)) status = Status.PASS;
        if(timer.milliseconds() > timeoutMs) status = Status.FAIL;
        if(gamepad1.xWasPressed()) status = Status.SKIP;
        if(gamepad1.yWasPressed()) status = Status.ERROR;
        if(status != Status.RUNNING) {
            note = noteSupplier.get(ctx);
            gamepad1.rumble(200);
            return true;
        }
        return false;
    }

    // A second longer than the tester's time limit, so this test's own FAIL is recorded first.
    @Override
    protected long timeoutMs() {
        return timeoutMs + 1000;
    }
}
