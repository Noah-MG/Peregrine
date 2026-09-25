package org.firstinspires.ftc.teamcode.tests;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Logger;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Drive;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Test auto: drive to one table target while logging the run to the SD card.</h3>
 *
 * <p>The target and starting pose are editable from FTC Dashboard. The log uses the same CSV format
 * as calibration, so a pathing run can also be used as fit data.</p>
 */
@Config
@Autonomous
public class PathingTest extends PeregrineOpMode {

    // Must match a target "name" in MANIFEST.JSON.
    public static String target = "score_left";
    // Starting pose in cm / rad, field frame. NOTE: the table's x/y span is inset from the field walls
    // (23..343 cm in the TABLE_FORMAT.MD §2 example). A start outside it reads as unreachable, so Drive
    // gets a zero command and falls straight to the PID.
    public static double x0 = 1;
    public static double y0 = 1;
    public static double h0 = 0.5;

    // The logger never finishes, so this ParallelTask keeps running after Drive arrives, until stop.
    @Override
    public Task defineTasks() {
        Drive drive = new Drive(this, target);
        Logger logger = new Logger(this);
        logger.addDrivetrainItems();

        return new ParallelTask(drive, logger);
    }

    @Override
    public void initStart() {

    }

    @Override
    public Pose2D startingPose() {
        return new Pose2D(DistanceUnit.CM, x0, y0, AngleUnit.RADIANS, h0);
    }

    @Override
    public void initLoop() {

    }

    @Override
    public void mainStart() {

    }

    @Override
    public void mainLoop() {

    }

    @Override
    public void end() {

    }
}
