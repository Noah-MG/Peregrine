package org.firstinspires.ftc.teamcode.peregrine.core.calibration;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class CalibrationMovement extends Task {

    PeregrineOpMode opMode;

    public static int smoothing = 2;

    public CalibrationMovement(PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    @Override
    public boolean run() {
        double fr = (opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x, smoothing - 1));
        double fl = (opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x, smoothing - 1));
        double br = (opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y - opMode.gamepad1.left_stick_x - opMode.gamepad1.right_stick_x, smoothing - 1));
        double bl = (opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x) * Math.abs(Math.pow(opMode.gamepad1.left_stick_y + opMode.gamepad1.left_stick_x + opMode.gamepad1.right_stick_x, smoothing - 1));

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

        return false;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return new CalibrationMovement(opMode);
    }
}
