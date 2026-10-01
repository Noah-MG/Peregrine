package org.firstinspires.ftc.teamcode.qa;

public interface QATask {
    /**
     * Does one loop's worth of the task. Called once per loop until it returns true, and must return
     * quickly. Set {@code status} (and optionally {@code note}) before returning true.
     *
     * <p>Throwing is also a way to finish: a QAContext.UnavailableSubsystem marks the task SKIP, an
     * AssertionError marks it FAIL, and anything else marks it ERROR.</p>
     *
     * @param ctx access to the robot and the tester for this task
     * @return true once the task has finished
     */
    boolean step(QAContext ctx);

    /**
     * Called once after the task finishes, however it finished, before QRRoutine.cleanupHardware().
     * Does nothing by default.
     */
    default void cleanup(QAContext ctx) {}

}
