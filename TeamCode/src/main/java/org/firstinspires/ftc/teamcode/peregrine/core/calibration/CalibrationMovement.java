package org.firstinspires.ftc.teamcode.peregrine.core.calibration;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Manual mecanum driving for the calibration run.</h3>
 *
 * <p>Left stick translates and right stick x turns. Each wheel's mixed power goes through a
 * sign-preserving power curve, {@code p * |p|^(smoothing - 1)}, and all four are then scaled down
 * together if any exceeds 1. The mixing matches Kinematics.powerMotors with
 * {@code y = left_stick_y, x = left_stick_x, h = right_stick_x}, but with the curve applied
 * per wheel instead of per stick.</p>
 */
public class CalibrationMovement extends Task {

    PeregrineOpMode opMode;

    // Exponent of the power curve. 1 is linear, 2 is sign-preserving square. (Not @Config, so not live-tunable.)
    public static int smoothing = 2;

    public CalibrationMovement(PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    @Override
    public boolean run() {
        // Mecanum mixing (forward +/- strafe +/- turn), then the sign-preserving power curve on each wheel.
        double fr = (opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x, smoothing - 1));
        double fl = (opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x, smoothing - 1));
        double br = (opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x, smoothing - 1));
        double bl = (opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x, smoothing - 1));

        // Scale all four together so none exceeds 100% and the direction of motion is preserved.
        double max = Math.max(Math.max(Math.max(Math.abs(fl), Math.abs(fr)), Math.abs(bl)), Math.abs(br));

        if (max > 1) {
            fl /= max;
            fr /= max;
            bl /= max;
            br /= max;
        }

        opMode.hardware.FL.setPower(fl);
        opMode.hardware.FR.setPower(fr);
        opMode.hardware.BL.setPower(bl);
        opMode.hardware.BR.setPower(br);

        // Never finishes on its own.
        return false;
    }

    // NOTE: does not stop the motors.
    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return new CalibrationMovement(opMode);
    }
}
