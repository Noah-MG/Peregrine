package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@TeleOp
public abstract class PeregrineTeleop extends PeregrineOpMode {

    Task teleop;

    public void initStart() {
        teleop = new ParallelTask(localizer, defineTasks());
    }

    /**
     * Define all of the tasks that you want the robot to go through during teleop, see examples.
     */
    public abstract Task defineTasks();

    public void mainLoop() {
        teleop.run();
    }
}
