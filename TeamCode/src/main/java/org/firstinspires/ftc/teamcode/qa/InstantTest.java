package org.firstinspires.ftc.teamcode.qa;


import androidx.annotation.Nullable;

/**
 * <h3>A test that runs your code once and reports a result straight away.</h3>
 *
 * <p>Good for checks that don't need time, such as the battery voltage:</p>
 * <pre>{@code
 * new InstantTest("Battery above 12.5 V", QATest.Category.HARDWARE,
 *         ctx -> ctx.hardwareMap().voltageSensor.iterator().next().getVoltage() + " V",
 *         ctx -> ctx.hardwareMap().voltageSensor.iterator().next().getVoltage() > 12.5
 *                 ? QATest.Status.PASS : QATest.Status.FAIL)
 * }</pre>
 */
public class InstantTest extends QATest {

    /** Builds the note written to the log once the test finishes. */
    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    /** Runs once and returns the test's final status. */
    public interface StepSupplier {
        Status run(QAContext ctx);
    }

    NoteSupplier noteSupplier;
    StepSupplier stepSupplier;

    /**
     * @param name the name shown on telemetry and written to the log
     * @param category what kind of thing the test checks
     * @param noteSupplier builds the log note; must not be null
     * @param stepSupplier runs once and returns PASS, FAIL, SKIP or ERROR
     */
    public InstantTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier) {
        super(name, category);
        this.noteSupplier = noteSupplier;
        this.stepSupplier = stepSupplier;
    }

    @Override
    protected boolean step(QAContext ctx) {
        status = Status.RUNNING;
        status = stepSupplier.run(ctx);
        note = noteSupplier.get(ctx);
        return true;
    }
}
