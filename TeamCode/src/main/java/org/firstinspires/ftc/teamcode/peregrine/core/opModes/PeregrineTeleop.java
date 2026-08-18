package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public abstract class PeregrineTeleop extends PeregrineOpMode {

    Task teleop;

    public void initStart() {
        teleop = new ParallelTask(localizer, defineTasks());
    }

    /**
     * Define all of the tasks that you want the robot to go through during teleop, see examples.
     */
    public abstract Task defineTasks();
    public abstract void finish();

    public boolean mainLoop() {
        return teleop.run();
    }

    public void end() {teleop.end(); finish();}
}
