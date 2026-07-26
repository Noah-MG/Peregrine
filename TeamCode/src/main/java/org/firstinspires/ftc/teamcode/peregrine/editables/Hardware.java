package org.firstinspires.ftc.teamcode.peregrine.editables;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;

public class Hardware {

    public GoBildaPinpointDriver odo;

    public DcMotor FR;
    public DcMotor FL;
    public DcMotor BR;
    public DcMotor BL;

    public Hardware(PeregrineOpMode opMode) {
        odo = opMode.hardwareMap.get(GoBildaPinpointDriver.class, "odo");
        odo.setOffsets(RobotParams.odoXOffset, RobotParams.odoYOffset, RobotParams.distanceUnit);
        odo.setEncoderResolution(RobotParams.podType);
        odo.setEncoderDirections(RobotParams.xEncoderDirection, RobotParams.yEncoderDirection);

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
