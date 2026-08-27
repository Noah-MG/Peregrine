package org.firstinspires.ftc.teamcode.peregrine.core.calibration;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineTeleop;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.ParallelRaceTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;
import org.firstinspires.ftc.teamcode.peregrine.editables.RobotParams;

@TeleOp
public class Calibration extends PeregrineTeleop {

    Task movement;
    Task logger;

    @Override
    public Task defineTasks() {
        movement = new CalibrationMovement(this);
        logger = new CalibrationLogger(this);

        return new ParallelRaceTask(movement, logger);
    }

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
    public void finish() {

    }
}
