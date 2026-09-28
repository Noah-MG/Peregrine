package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Runs a piece of code once, then is immediately finished.</h3>
 *
 * <p>Useful for one-off actions that don't need a class of their own, such as setting a servo
 * position. The code is usually written as a lambda:</p>
 * <pre>{@code
 * new InstantTask(() -> hardware.claw.setPosition(1))
 * }</pre>
 * <p>The code runs on the task's first run(), not when the task is built.</p>
 */
@Ends
public class InstantTask extends Task {

    Runnable runnable;

    /**
     * @param runnable the code to run, usually a lambda such as {@code () -> doSomething()}
     */
    public InstantTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public boolean run() {
        runnable.run();
        return true;
    }

    @Override
    public void end() {

    }

    @Override
    public Task reset() {
        return new InstantTask(runnable);
    }
}
