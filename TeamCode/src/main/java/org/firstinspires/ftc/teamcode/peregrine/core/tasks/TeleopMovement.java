package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Kinematics;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class TeleopMovement extends Task {

    PeregrineOpMode opMode;

    public TeleopMovement(PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    @Override
    public boolean run() {
        Kinematics.powerMotors(Math.pow(opMode.gamepad1.left_stick_x, 3), Math.pow(opMode.gamepad1.left_stick_y, 3), Math.pow(opMode.gamepad1.right_stick_x, 3), opMode);
        return false;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return new TeleopMovement(opMode);
    }
}
