package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@Autonomous
public abstract class PeregrineAutonomous extends PeregrineOpMode {

    Task auto;

    public void initStart() {
        auto = new ParallelTask(localizer, defineTasks());
    }

    /**
     * Define all of the tasks that you want the robot to go through during auto, see examples.
     */
    public abstract Task defineTasks();

    public boolean mainLoop() {
        return auto.run();
    }

}
