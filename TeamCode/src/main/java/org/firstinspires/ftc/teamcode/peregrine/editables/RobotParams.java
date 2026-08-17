package org.firstinspires.ftc.teamcode.peregrine.editables;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Enums;

@Config
public final class RobotParams {

    public static Enums.Chassis chassis = Enums.Chassis.MECANUM;

    public static DistanceUnit distanceUnit = DistanceUnit.CM;

    public static double odoXOffset = -0.04161;
    public static double odoYOffset = 0.18346;
    public static GoBildaPinpointDriver.GoBildaOdometryPods podType = GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD;
    public static GoBildaPinpointDriver.EncoderDirection xEncoderDirection = GoBildaPinpointDriver.EncoderDirection.FORWARD;
    public static GoBildaPinpointDriver.EncoderDirection yEncoderDirection = GoBildaPinpointDriver.EncoderDirection.FORWARD;


    public static DcMotor.Direction FRDirection = DcMotor.Direction.REVERSE;
    public static DcMotor.Direction FLDirection = DcMotor.Direction.FORWARD;
    public static DcMotor.Direction BRDirection = DcMotor.Direction.REVERSE;
    public static DcMotor.Direction BLDirection = DcMotor.Direction.FORWARD;

}
