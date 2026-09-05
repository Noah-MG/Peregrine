package org.firstinspires.ftc.teamcode.tests;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.calibration.CalibrationLogger;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineAutonomous;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Drive;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@Config
@Autonomous
public class PathingTest extends PeregrineAutonomous {

    public static String target = "score_left";
    public static double x0 = 1;
    public static double y0 = 1;
    public static double h0 = 0.5;

    @Override
    public Task defineTasks() {
        return new ParallelTask(new Drive(this, target), new CalibrationLogger(this));
    }

    @Override
    public void finish() {

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
}
