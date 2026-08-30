package org.firstinspires.ftc.teamcode.tests;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.calibration.CalibrationLogger;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineAutonomous;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Drive;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@Autonomous
public class TableReading extends PeregrineAutonomous {
    @Override
    public Task defineTasks() {
        return new ParallelTask(new Drive(this, "score_left"), new CalibrationLogger(this));
    }

    @Override
    public void finish() {

    }

    @Override
    public Pose2D startingPose() {
        return new Pose2D(DistanceUnit.CM, 1, 1, AngleUnit.RADIANS, 0.5);
    }

    @Override
    public void initLoop() {

    }

    @Override
    public void mainStart() {

    }
}
