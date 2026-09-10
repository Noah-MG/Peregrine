package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Base class for autonomous opModes.</h3>
 *
 * <p>Subclasses return their whole routine from {@link #defineTasks()}. That routine runs in parallel
 * with the localizer, so odometry updates every loop.</p>
 *
 * <p>The Localizer task never returns true, so the ParallelTask never finishes either. The opMode
 * therefore runs until it is stopped, even after the user's routine has completed.</p>
 */
public abstract class PeregrineAutonomous extends PeregrineOpMode {

    // Localizer + the user's routine, run together.
    Task auto;

    public void initStart() {
        auto = new ParallelTask(localizer, defineTasks());
    }

    /**
     * Define all of the tasks that you want the robot to go through during auto, see examples.
     */
    public abstract Task defineTasks();
    /** Called once at the very end of the opMode, after the task tree has been ended. */
    public abstract void finish();

    // One tick of the task tree. Returning true ends the main loop in PeregrineOpMode.
    public boolean mainLoop() {
        return auto.run();
    }

    public void end() {auto.end(); finish();}

}
