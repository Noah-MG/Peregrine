package org.firstinspires.ftc.teamcode.qa;

/**
 * <h3>A test that runs your code every loop until it reports a result.</h3>
 *
 * <p>The step returns {@code Status.RUNNING} to keep going, or PASS, FAIL, SKIP or ERROR to finish
 * with that status. For example, a test that a motor's encoder counts up:</p>
 * <pre>{@code
 * new SimpleTest("Lift encoder", QATest.Category.HARDWARE,
 *         ctx -> "ticks: " + ctx.hardware().lift.getCurrentPosition(),
 *         ctx -> {
 *             ctx.hardware().lift.setPower(0.3);
 *             if (ctx.hardware().lift.getCurrentPosition() > 100) return QATest.Status.PASS;
 *             return QATest.Status.RUNNING;
 *         },
 *         ctx -> ctx.hardware().lift.setPower(0))
 * }</pre>
 */
public class SimpleTest extends QATest {

    /** Builds the note written to the log once the test finishes. */
    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    /** Runs once per loop and returns RUNNING to continue, or the test's final status. */
    public interface StepSupplier {
        Status run(QAContext ctx);
    }

    /** Runs once after the test finishes, for example to turn a motor off. */
    public interface CleanupSupplier {
        void run(QAContext ctx);
    }

    NoteSupplier noteSupplier;
    StepSupplier stepSupplier;
    CleanupSupplier cleanupSupplier;
    long timeout;
    boolean firstCall;

    /**
     * Creates a test with the default timeout ({@link QATest#defaultTimeout}).
     *
     * @param name the name shown on telemetry and written to the log
     * @param category what kind of thing the test checks
     * @param noteSupplier builds the log note when the test finishes, or null for no note
     * @param stepSupplier runs once per loop; returns RUNNING to continue or the final status
     * @param cleanupSupplier runs after the test finishes, or null for no cleanup
     */
    public SimpleTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, CleanupSupplier cleanupSupplier) {
        this(name, category, noteSupplier, stepSupplier, cleanupSupplier, QATest.defaultTimeout);
    }

    /**
     * Creates a test with its own timeout.
     *
     * @param timeout how long the test may run before it fails as "TIMED OUT", in milliseconds
     */
    public SimpleTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, CleanupSupplier cleanupSupplier, long timeout) {
        super(name, category);
        this.noteSupplier = noteSupplier;
        this.stepSupplier = stepSupplier;
        this.cleanupSupplier = cleanupSupplier;
        firstCall = true;
        this.timeout = timeout;
    }

    @Override
    public boolean step(QAContext ctx) {
        if(firstCall) {
            status = Status.RUNNING;
            firstCall = false;
        }
        Status s = stepSupplier.run(ctx);
        if (s == null || s == Status.PENDING) status = Status.ERROR;
        else if (s != Status.RUNNING) {
            status = s;
            if (noteSupplier != null) note = noteSupplier.get(ctx);
            return true;
        }
        return false;
    }

    @Override
    public void cleanup(QAContext ctx) {
        if (cleanupSupplier != null) cleanupSupplier.run(ctx);
    }

    @Override
    protected long timeoutMs() {
        return timeout;
    }
}
