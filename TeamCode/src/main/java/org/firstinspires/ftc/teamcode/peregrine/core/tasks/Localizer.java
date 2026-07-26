package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class Localizer extends Task {

    PeregrineOpMode opMode;

    Pose2D startingPose;

    public Localizer (PeregrineOpMode opMode, Pose2D startingPose) {
        this.opMode = opMode;
        this.startingPose = startingPose;
        int i = 0;
        opMode.hardware.odo.resetPosAndIMU();
        while(opMode.hardware.odo.getDeviceStatus() != GoBildaPinpointDriver.DeviceStatus.READY) {
            opMode.hardware.odo.update();
            opMode.telem.addData("Odo Status", opMode.hardware.odo.getDeviceStatus());
            opMode.telem.addData("Loop Number", i);
            opMode.telem.update();
            i++; // TODO: figure out why this doesn't work properly all the time and fix it.
        }
        opMode.hardware.odo.setPosition(startingPose);
    }

    public Pose2D getPose() {
        return opMode.hardware.odo.getPosition();
    }

    public double getVelX(DistanceUnit distanceUnit) {
        return opMode.hardware.odo.getVelX(distanceUnit);
    }

    public double getVelY(DistanceUnit distanceUnit) {
        return opMode.hardware.odo.getVelY(distanceUnit);
    }

    public double getHeadingVelocity(UnnormalizedAngleUnit angleUnit) {
        return opMode.hardware.odo.getHeadingVelocity(angleUnit);
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return new Localizer(opMode, startingPose);
    }

}
