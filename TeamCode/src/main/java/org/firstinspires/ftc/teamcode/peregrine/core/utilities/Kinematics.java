package org.firstinspires.ftc.teamcode.peregrine.core.utilities;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.editables.RobotParams;

public final class Kinematics {

    public static void powerMotors(double y, double x, double h, PeregrineOpMode opMode) {
        switch(RobotParams.chassis) {
            case MECANUM:

                // Combine the joystick requests for each axis-motion to determine each wheel's power.
                double fr = y + x - h;
                double fl = y - x + h;
                double br = y - x - h;
                double bl = y + x + h;

                // Normalize the values so no wheel power exceeds 100%
                double max = Math.max(Math.max(Math.max(Math.abs(fr), Math.abs(fl)), Math.abs(br)), Math.abs(bl));

                if (max > 1) {
                    fr /= max;
                    fl /= max;
                    br /= max;
                    bl /= max;
                }

                opMode.hardware.FR.setPower(fr);
                opMode.hardware.FL.setPower(fl);
                opMode.hardware.BR.setPower(br);
                opMode.hardware.BL.setPower(bl);

                break;
            default:
                //freak the frickity flip out and crash and burn and explode
                break;
        }
    }

}
