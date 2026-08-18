package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public abstract class PeregrineAutonomous extends PeregrineOpMode {

    Task auto;

    public void initStart() {
        auto = new ParallelTask(localizer, defineTasks());
    }

    /**
     * Define all of the tasks that you want the robot to go through during auto, see examples.
     */
    public abstract Task defineTasks();
    public abstract void finish();

    public boolean mainLoop() {
        return auto.run();
    }

    public void end() {auto.end(); finish();}

}
