package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Wraps the goBILDA Pinpoint odometry computer.</h3>
 *
 * <p>Runs as a never-ending task alongside every opMode's task tree, see PeregrineAutonomous and
 * PeregrineTeleop. run() pulls a fresh reading from the Pinpoint each loop, and the getters return
 * that cached reading.</p>
 *
 * <p>All values are field frame, which is what the value tables expect (TABLE_FORMAT.MD §3). The
 * drivetrain model and PID are body frame, so their callers rotate by -h themselves.</p>
 */
public class Localizer extends Task {

    Pose2D startingPose;

    /**
     * Resets the Pinpoint and its IMU, blocks until it reports READY, then sets its pose to
     * startingPose. Runs during init. Nothing checks for a stop request, so if the Pinpoint never
     * becomes READY this loop hangs.
     */
    public Localizer (PeregrineOpMode opMode, Pose2D startingPose) {
        this.opMode = opMode;
        this.startingPose = startingPose;
        // Loop counter, only shown on telemetry while waiting.
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

    /** Field-frame pose from the most recent update(). */
    public Pose2D getPose() {
        return opMode.hardware.odo.getPosition();
    }

    /** Field-frame x velocity. */
    public double getVelX(DistanceUnit distanceUnit) {
        return opMode.hardware.odo.getVelX(distanceUnit);
    }

    /** Field-frame y velocity. */
    public double getVelY(DistanceUnit distanceUnit) {
        return opMode.hardware.odo.getVelY(distanceUnit);
    }

    /** Angular velocity. It is the same in the field and body frames. */
    public double getHeadingVelocity(UnnormalizedAngleUnit angleUnit) {
        return opMode.hardware.odo.getHeadingVelocity(angleUnit);
    }

    /**
     * The full 6D state in the value tables' axis order and units (TABLE_FORMAT.MD §3):
     * {@code [x cm, y cm, h rad, vx cm/s, vy cm/s, w rad/s]}, field frame.
     */
    public double[] getStateVector() {
        double[] output = new double[6];
        Pose2D pose = getPose();
        output[0] = pose.getX(DistanceUnit.CM);
        output[1] = pose.getY(DistanceUnit.CM);
        output[2] = pose.getHeading(AngleUnit.RADIANS);
        output[3] = getVelX(DistanceUnit.CM);
        output[4] = getVelY(DistanceUnit.CM);
        output[5] = getHeadingVelocity(UnnormalizedAngleUnit.RADIANS);
        return output;
    }

    // Pulls a fresh reading from the Pinpoint. Never finishes.
    @Override
    public boolean run() {
        opMode.hardware.odo.update();
        return false;
    }

    @Override
    public boolean end() {
        return false;
    }

    // NOTE: constructing a new Localizer resets the Pinpoint and puts the pose back at startingPose.
    @Override
    public Task reset() {
        return new Localizer(opMode, startingPose);
    }

}
