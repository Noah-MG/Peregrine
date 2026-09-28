package org.firstinspires.ftc.teamcode.qa;

import androidx.annotation.Nullable;

/**
 * <h3>One check in a QA routine.</h3>
 *
 * <p>A QRRoutine runs its tests one at a time. While a test is active, its {@link #step(QAContext)}
 * is called once per loop until it returns true, the test times out, or the opMode is stopped. The
 * test's name, category, final status and note are then written as one row of the QA log.</p>
 *
 * <p>Most tests don't need their own class. Use one of the ready-made kinds instead:</p>
 * <ul>
 *     <li>{@link SimpleTest}: runs code every loop until it reports a result.</li>
 *     <li>{@link InstantTest}: runs code once and reports a result straight away.</li>
 *     <li>{@link ManualCheckTest}: the tester watches the robot and presses a button to grade it.</li>
 *     <li>{@link OperatorActionTest}: asks the tester to do something, and passes when the code sees
 *     it happen.</li>
 * </ul>
 */
public abstract class QATest {
    /** What a test checks: code alone, a physical part of the robot, or both working together. */
    public enum Category { SOFTWARE, HARDWARE, INTEGRATED }

    /**
     * The state of a test. PENDING before it starts and RUNNING while it runs; it finishes as one of
     * PASS, FAIL, SKIP (couldn't or shouldn't be tested) or ERROR (the test itself went wrong).
     */
    public enum Status { PENDING, RUNNING, PASS, FAIL, SKIP, ERROR }

    /** The name shown on telemetry and written to the log. */
    public final String name;
    public final Category category;
    // Set by the test while it runs, and by QRRoutine if it times out, is stopped, or throws.
    Status status = Status.PENDING;
    /** Optional extra information written to the log's Note column, such as a measured value. */
    @Nullable public String note;
    /** How long a test may run before it fails as "TIMED OUT", in milliseconds, unless it overrides timeoutMs(). */
    public static final long defaultTimeout = 15000;

    /**
     * @param name the name shown on telemetry and written to the log
     * @param category what kind of thing the test checks
     */
    public QATest(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    /**
     * Does one loop's worth of the test. Called once per loop until it returns true, and must return
     * quickly. Set {@code status} (and optionally {@code note}) before returning true.
     *
     * <p>Throwing is also a way to finish: a QAContext.UnavailableSubsystem marks the test SKIP, an
     * AssertionError marks it FAIL, and anything else marks it ERROR.</p>
     *
     * @param ctx access to the robot and the tester for this test
     * @return true once the test has finished
     */
    protected abstract boolean step(QAContext ctx);

    /**
     * Called once after the test finishes, however it finished, before QRRoutine.cleanupHardware().
     * Does nothing by default.
     */
    protected void cleanup(QAContext ctx) {}

    /** @return how long the test may run before it fails as "TIMED OUT", in milliseconds */
    protected long timeoutMs() { return defaultTimeout; }
}

