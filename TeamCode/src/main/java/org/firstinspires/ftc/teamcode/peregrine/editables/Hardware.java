package org.firstinspires.ftc.teamcode.peregrine.editables;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;

/**
 * <h3>All robot hardware, mapped once per opMode.</h3>
 *
 * <p>The names ("odo", "FR", "FL", "BR", "BL") must match the Driver Station robot configuration.
 * Add your own mechanisms here. Tuning values live in RobotParams.</p>
 */
public class Hardware {

    // goBILDA Pinpoint odometry computer, read by Localizer.
    public GoBildaPinpointDriver odo;

    // Drive motors: front-right, front-left, back-right, back-left.
    public DcMotor FR;
    public DcMotor FL;
    public DcMotor BR;
    public DcMotor BL;

    public Hardware(PeregrineOpMode opMode) {
        odo = opMode.hardwareMap.get(GoBildaPinpointDriver.class, "odo");
        // Pod positions relative to the robot's tracking centre. The drivetrain fit is sensitive to this
        // (TABLE_FORMAT.MD §8.6 notes a mis-set tracking point leaking into the fit).
        odo.setOffsets(RobotParams.odoXOffset, RobotParams.odoYOffset, RobotParams.distanceUnit);
        odo.setEncoderResolution(RobotParams.podType);
        odo.setEncoderDirections(RobotParams.xEncoderDirection, RobotParams.yEncoderDirection);

        // Open-loop power control: the drivetrain model was fitted against raw power, not encoder velocity.
        FR = opMode.hardwareMap.get(DcMotor.class, "FR");
        FR.setDirection(RobotParams.FRDirection);
        FR.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        FL = opMode.hardwareMap.get(DcMotor.class, "FL");
        FL.setDirection(RobotParams.FLDirection);
        FL.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        BR = opMode.hardwareMap.get(DcMotor.class, "BR");
        BR.setDirection(RobotParams.BRDirection);
        BR.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        BL = opMode.hardwareMap.get(DcMotor.class, "BL");
        BL.setDirection(RobotParams.BLDirection);
        BL.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

}
