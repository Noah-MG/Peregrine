package org.firstinspires.ftc.teamcode.tests;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineAutonomous;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Drive;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@Autonomous
public class TableReading extends PeregrineAutonomous {
    @Override
    public Task defineTasks() {
        return new Drive(this);
    }

    @Override
    public void finish() {

    }

    @Override
    public Pose2D startingPose() {
        return new Pose2D(DistanceUnit.CM, 0, 0, AngleUnit.RADIANS, 0);
    }

    @Override
    public void initLoop() {

    }

    @Override
    public void mainStart() {

    }
}
