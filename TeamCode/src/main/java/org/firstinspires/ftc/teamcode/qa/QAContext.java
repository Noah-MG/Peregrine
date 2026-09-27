package org.firstinspires.ftc.teamcode.qa;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Localizer;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.OptimalityEngine;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;
import org.firstinspires.ftc.teamcode.peregrine.editables.GlobalVariables;
import org.firstinspires.ftc.teamcode.peregrine.editables.Hardware;

public class QAContext {

    public static class QAEnvironment extends PeregrineOpMode {

        public QAEnvironment() {}

        @Override
        public Pose2D startingPose() {
            return null;
        }

        @Override
        public Task defineTasks() {
            return null;
        }

        @Override
        public void initStart() {

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

    public static class UnavailableSubsystem extends RuntimeException {
        public UnavailableSubsystem(String message) {
            super(message);
        }
        public UnavailableSubsystem(Throwable cause) {
            super(cause);
        }
        public UnavailableSubsystem(String message, Throwable cause) {
            super(message, cause);
        }
    }

    QAEnvironment environment;
    QRRoutine opMode;
    QATest test;

    Telemetry.Item header;
    Telemetry.Item instruction;

    ElapsedTime timer;

    public QAContext(QRRoutine opMode, QATest test) {
        this.opMode = opMode;
        this.test = test;
        environment = new QAEnvironment();
        environment.hardwareMap = opMode.hardwareMap;
        if (opMode.telem == null) throw new IllegalArgumentException("Define telem in QRRoutine before opening QAContext");
        environment.telem = opMode.telem;
        environment.gamepad1 = opMode.gamepad1;
        environment.gamepad2 = opMode.gamepad2;
        header = opMode.telem.addData("TEST", () ->
                test.category + " [" + test.status + "]: " + test.name);
        instruction = opMode.telem.addData("Instruction", "none");
        timer = new ElapsedTime();

        opMode.gamepad1.aWasPressed();
        opMode.gamepad1.bWasPressed();
        opMode.gamepad1.xWasPressed();
        opMode.gamepad1.yWasPressed();
    }

    public PeregrineOpMode opMode() {
        return environment;
    }

    public HardwareMap hardwareMap() {
        return environment.hardwareMap;
    }

    public Hardware hardware() throws UnavailableSubsystem {
        if (environment.hardware == null) {
            try {
                environment.hardware = new Hardware(environment);
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Hardware failed to initialize", t);
            }
        }

        return environment.hardware;
    }

    public Localizer localizer() throws UnavailableSubsystem {
        if (environment.localizer == null) {
            if(environment.hardware == null) hardware();
            try {
                environment.localizer = new Localizer(environment,
                        new Pose2D(DistanceUnit.CM, 0, 0, AngleUnit.RADIANS, 0));
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Localizer failed to initialize", t);
            }
        }

        return environment.localizer;
    }

    public OptimalityEngine optimalityEngine() throws UnavailableSubsystem {
        if (environment.optimalityEngine == null) {
            try {
                environment.optimalityEngine = new OptimalityEngine(environment);
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Optimality Engine failed to initialize", t);
            }
        }

        return environment.optimalityEngine;
    }

    public Telemetry telem() {
        return environment.telem;
    }

    public GlobalVariables globalVariables() {
        if (environment.globalVariables == null) {
            try {
                environment.globalVariables = new GlobalVariables();
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Global Variables failed to initialize", t);
            }
        }

        return environment.globalVariables;
    }



    public void prompt(String instruction) {
        this.instruction.setValue(instruction);
    }

    public Gamepad gamepad1() {
        return opMode.gamepad1;
    }

    public Gamepad gamepad2() {
        return opMode.gamepad2;
    }



    public double testSeconds() {
        return timer.seconds();
    }

    public boolean after(double seconds) {
        return timer.seconds() > seconds;
    }



    protected boolean tick() {
        if (environment.localizer != null) environment.localizer.run();
        return test.step(this);
    }

    protected void close() {
        try { test.cleanup(this); } catch (Throwable ignored) {}
        try { opMode.cleanupHardware(this, environment.hardware != null); }
        catch (Throwable ignored) {}
        if(environment.optimalityEngine != null) try { environment.optimalityEngine.closeReaders(); }
        catch (Throwable ignored) {}
    }
}
