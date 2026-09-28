package org.firstinspires.ftc.teamcode.peregrine.core.utilities;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;

/**
 *  <h3>The class that represents any action the robot might take, no matter how complicated</h3>
 *
 *  <p>Examples of this include: changing a servo position, moving across the field, or running an
 *  entire autonomous. A task never blocks. Instead, its run() method is called once per loop, does a
 *  small amount of work, and reports whether the task has finished.</p>
 *
 *  <p>Tasks are composed into trees with SeriesTask, ParallelTask and ParallelRaceTask. The root is
 *  returned by PeregrineOpMode.defineTasks() and ticked once per loop, raced against the Localizer.
 *  When the root finishes, the opMode ends.</p>
 *
 *  <p>To write your own task, extend this class, set {@link #opMode} in the constructor, implement
 *  run(), end() and reset(), and add {@link Ends @Ends} to the class if it always finishes on its own.</p>
 */

public abstract class Task {

    /**
     * The opMode this task belongs to. Through it a task can reach the hardware, localizer, telemetry,
     * gamepads and global variables. Subclasses set it in their constructor. Tasks that don't need the
     * robot, such as InstantTask and WaitUntilTask, leave it null.
     */
    protected PeregrineOpMode opMode;

    /**
     * Does one loop's worth of work. Called once per loop until it returns true. It must return quickly,
     * so never sleep or wait inside it.
     *
     * @return Whether the task has finished. Unending tasks always return false.
     */
    public abstract boolean run();

    /**
     * Puts the task into a safe end state when it is stopped before it has finished, for example when it
     * loses a ParallelRaceTask or when a TeleopTask's button is released. Typically this turns motors
     * off. It is not called on a task whose run() has already returned true.
     */
    public abstract void end();

    /**
     * Returns a fresh copy of the task, as if it had never run. TeleopTask uses this to run the same task
     * again on the next button press, and compound tasks reset each of their children.
     *
     * @return a fresh copy of the task
     */
    public abstract Task reset();

    /**
     * Returns whether this task is guaranteed to finish on its own. By default this is true exactly when
     * the class is annotated with {@link Ends @Ends}. CompoundTask subclasses override it to check their
     * children instead.
     *
     * @return Whether this task ends
     */
    public boolean ends() {
        return this.getClass().isAnnotationPresent(Ends.class);
    }
}
