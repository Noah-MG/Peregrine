package org.firstinspires.ftc.teamcode.peregrine.core.calibration;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Logger;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelRaceTask;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.TeleopMovement;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;
import org.firstinspires.ftc.teamcode.peregrine.editables.RobotParams;

/**
 * <h3>Drivetrain calibration teleop</h3>
 *
 * <p>The driver drives by hand while every loop's motor powers, pose and velocity are written to a CSV
 * on the SD card. The desktop fitter ({@code calibration/fit_drivetrain.py}) regresses those logs into
 * the drivetrain model stored in MODEL.JSON (TABLE_FORMAT.MD §8.6). Drive on the same surface you
 * will compete on, and make sure some of the run pushes past the traction limit so the knee can be fit.</p>
 *
 * <p>Note: PeregrineOpMode always constructs an OptimalityEngine, so this opMode also stops if the
 * SD card has no MANIFEST.JSON / MODEL.JSON.</p>
 */
@TeleOp
public class Calibration extends PeregrineOpMode {

    TeleopMovement movement;
    Logger logger;

    @Override
    public Task defineTasks() {
        movement = new TeleopMovement(this);
        logger = new Logger(this);
        logger.addLogItem("FR", () -> hardware.FR.getPower());
        logger.addLogItem("FL", () -> hardware.FL.getPower());
        logger.addLogItem("BR", () -> hardware.BR.getPower());
        logger.addLogItem("BL", () -> hardware.BL.getPower());
        logger.addLogItem("x",  () -> localizer.getPose().getX(DistanceUnit.CM));
        logger.addLogItem("y",  () -> localizer.getPose().getY(DistanceUnit.CM));
        logger.addLogItem("h",  () -> localizer.getPose().getHeading(AngleUnit.RADIANS));
        logger.addLogItem("x_vel", () -> localizer.getVelX(DistanceUnit.CM));
        logger.addLogItem("y_vel", () -> localizer.getVelY(DistanceUnit.CM));
        logger.addLogItem("h_vel", () -> localizer.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS));

        // Neither task ever returns true, so the race runs until the opMode is stopped.
        return new ParallelRaceTask(movement, logger);
    }

    @Override
    public void initStart() {

    }

    // Calibration only needs relative motion, so start at the origin.
    @Override
    public Pose2D startingPose() {
        return new Pose2D(RobotParams.distanceUnit, 0, 0, AngleUnit.RADIANS, 0);
    }

    @Override
    public void initLoop() {

    }

    @Override
    public void mainStart() {

    }

    @Override
    public boolean mainLoop() {
        return false;
    }

    @Override
    public void end() {

    }
}
