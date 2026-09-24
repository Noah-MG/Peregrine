package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Kinematics;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Basic robot-centric mecanum teleop from gamepad1.</h3>
 *
 * <p>Each stick axis is cubed, which keeps its sign but gives finer control near centre, before it is
 * sent to Kinematics.powerMotors. Never finishes on its own.</p>
 */
public class TeleopMovement extends Task {

    public TeleopMovement(PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    @Override
    public boolean run() {
        // powerMotors(y = forward, x = strafe, h = turn).
        Kinematics.powerMotors(Math.pow(opMode.gamepad1.left_stick_y, 3), Math.pow(opMode.gamepad1.left_stick_x, 3), Math.pow(opMode.gamepad1.right_stick_x, 3), opMode);
        return false;
    }

    // NOTE: does not stop the motors.
    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return new TeleopMovement(opMode);
    }
}
