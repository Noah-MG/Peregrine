package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.util.function.BooleanSupplier;

/**
 * <h3>Binds a task to a button for teleop.</h3>
 *
 * <p>The button is any condition, usually a gamepad button written as a lambda such as
 * {@code () -> gamepad1.a}. Pressing it starts the task. What happens next depends on
 * {@code togglable}:</p>
 * <ul>
 *     <li><b>false:</b> the task runs only while the button is held. Releasing the button ends the
 *     task early.</li>
 *     <li><b>true:</b> the task keeps running after the button is released, until it finishes. The
 *     task must be one that ends (see Task.ends()).</li>
 * </ul>
 * <p>After the task finishes or is ended, it is reset so the next press starts it fresh. A TeleopTask
 * never finishes on its own, so run several of them, alongside TeleopMovement, in a ParallelTask.</p>
 */
public class TeleopTask extends Task {

    // The task bound to the button. Replaced with task.reset() after each use.
    Task task;
    // The button (or other condition) that starts the task.
    BooleanSupplier isPushed;
    boolean togglable;
    // Whether the bound task is currently running.
    boolean toggled;

    // Button state this loop and last loop, used to detect presses and releases.
    boolean isPushedCurr;
    boolean isPushedPrev;

    /**
     * @param opMode the running opMode
     * @param task the task to run when the button is pressed
     * @param isPushed the button, usually a lambda such as {@code () -> gamepad1.a}
     * @param togglable false to run the task only while the button is held; true to let it run to
     *                  completion after a single press
     * @throws IllegalArgumentException if togglable is true and the task doesn't end.
     */
    public TeleopTask(PeregrineOpMode opMode, Task task, BooleanSupplier isPushed, boolean togglable) {
        this.opMode = opMode;
        this.task = task;
        this.isPushed = isPushed;
        this.togglable = togglable;
        toggled = false;
        isPushedCurr = false;
        isPushedPrev = false;
        if (togglable && !task.ends()) {
            throw new IllegalArgumentException("Invalid task, for togglable TeleopTasks ensure the selected task can end.");
        }
    }

    // Starts the task on a press, runs it while active, and resets it when it finishes. In hold mode a
    // release also ends it. Never finishes.
    @Override
    public boolean run() {
        isPushedCurr = isPushed.getAsBoolean();
        if (!togglable) {
            if (isPushedCurr && !isPushedPrev) toggled = true;
            if (toggled) if(task.run()) { toggled = false; task = task.reset(); }
            if (isPushedPrev && !isPushedCurr && toggled) { toggled = false; task.end(); task = task.reset(); }
        } else {
            if (isPushedCurr && !isPushedPrev) toggled = true;
            if (toggled) if(task.run()) { toggled = false; task = task.reset(); }
        }
        isPushedPrev = isPushedCurr;
        return false;
    }

    // Ends the bound task if it is currently running.
    @Override
    public void end() {
        if (toggled) if (!task.run()) task.end();
    }

    @Override
    public Task reset() {
        return new TeleopTask(opMode, task.reset(), isPushed, togglable);
    }
}
