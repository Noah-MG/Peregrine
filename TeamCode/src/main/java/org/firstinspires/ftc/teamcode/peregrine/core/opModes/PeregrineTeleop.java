package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Base class for teleop opModes.</h3>
 *
 * <p>Behaves exactly like PeregrineAutonomous: the task tree from {@link #defineTasks()} runs in
 * parallel with the localizer until the opMode is stopped.</p>
 */
public abstract class PeregrineTeleop extends PeregrineOpMode {

    // Localizer + the user's teleop tasks, run together.
    Task teleop;

    public void initStart() {
        teleop = new ParallelTask(localizer, defineTasks());
    }

    /**
     * Define all of the tasks that you want the robot to go through during teleop, see examples.
     */
    public abstract Task defineTasks();
    /** Called once at the very end of the opMode, after the task tree has been ended. */
    public abstract void finish();

    // One tick of the task tree. Returning true ends the main loop in PeregrineOpMode.
    public boolean mainLoop() {
        return teleop.run();
    }

    public void end() {teleop.end(); finish();}
}
