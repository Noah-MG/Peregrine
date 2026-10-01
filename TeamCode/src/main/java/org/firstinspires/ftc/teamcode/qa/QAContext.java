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

/**
 * <h3>Everything a QA test can reach while it runs.</h3>
 *
 * <p>A new context is made for every test. The robot's subsystems (hardware, localizer,
 * OptimalityEngine, global variables) are only built the first time a test asks for them. If one
 * fails to build, an UnavailableSubsystem is thrown and the test is marked SKIP rather than ERROR, so a
 * missing device doesn't hide other results.</p>
 *
 * <p>It also lets a test talk to the tester ({@link #prompt(String)}, the gamepads) and keep time
 * ({@link #testSeconds()}, {@link #after(double)}).</p>
 */
public class QAContext {

    /**
     * A stand-in PeregrineOpMode that is never run. It holds the subsystems a test builds, and can be
     * passed to Peregrine tasks that need an opMode, see {@link #opMode()}.
     */
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

    /** Thrown when a subsystem can't be built. QRRoutine marks the test SKIP. */
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
    public QRRoutine opMode;
    QATask task;
    public QATest test;
    public boolean isTest;

    // Time since this test started.
    ElapsedTime timer;

    /**
     * Sets up a fresh environment for one test, sharing the routine's hardwareMap, telemetry and
     * gamepads.
     *
     * @throws IllegalArgumentException if the routine's telemetry hasn't been set up yet.
     */
    public QAContext(QRRoutine opMode, QATask task) {
        this.opMode = opMode;
        this.task = task;
        if(task instanceof QATest) {
            isTest = true;
            test = (QATest) task;
            environment = new QAEnvironment();
            environment.hardwareMap = opMode.hardwareMap;
            if (opMode.telem == null)
                throw new IllegalArgumentException("Define telem in QRRoutine before opening QAContext");
            environment.telem = opMode.telem;
            environment.gamepad1 = opMode.gamepad1;
            environment.gamepad2 = opMode.gamepad2;
        } else {
            isTest = false;
        }
        timer = new ElapsedTime();

        // Clear any button presses left over from the previous test.
        opMode.gamepad1.aWasPressed();
        opMode.gamepad1.bWasPressed();
        opMode.gamepad1.xWasPressed();
        opMode.gamepad1.yWasPressed();
    }

    /**
     * The test's stand-in opMode, for building Peregrine tasks inside a test, for example
     * {@code new WaitTask(ctx.opMode(), 500)}. Call hardware() or localizer() first if the task needs them.
     */
    public PeregrineOpMode opMode() {
        if(!isTest) throw new IllegalStateException("This is not a test, there is no environment");
        return environment;
    }

    /** The FTC hardwareMap, for devices that aren't in Hardware. */
    public HardwareMap hardwareMap() {
        if(!isTest) throw new IllegalStateException("This is not a test, there is no environment");
        return environment.hardwareMap;
    }

    /**
     * The robot's Hardware, built on first use.
     * @throws UnavailableSubsystem if it can't be built, for example when a device is missing from the configuration.
     */
    public Hardware hardware() throws UnavailableSubsystem {
        if(!isTest) throw new IllegalStateException("This is not a test, there is no environment");
        if (environment.hardware == null) {
            try {
                environment.hardware = new Hardware(environment);
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Hardware failed to initialize", t);
            }
        }

        return environment.hardware;
    }

    /**
     * A Localizer starting at (0, 0) cm, heading 0, built on first use along with the hardware. Once
     * built, it is updated every loop before the test's step.
     * @throws UnavailableSubsystem if it or the hardware can't be built.
     */
    public Localizer localizer() throws UnavailableSubsystem {
        if(!isTest) throw new IllegalStateException("This is not a test, there is no environment");
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

    /**
     * The OptimalityEngine, built on first use. This reads the tables from the SD card.
     * @throws UnavailableSubsystem if the card or tables are missing or invalid.
     */
    public OptimalityEngine optimalityEngine() throws UnavailableSubsystem {
        if(!isTest) throw new IllegalStateException("This is not a test, there is no environment");
        if (environment.optimalityEngine == null) {
            try {
                environment.optimalityEngine = new OptimalityEngine(environment);
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Optimality Engine failed to initialize", t);
            }
        }

        return environment.optimalityEngine;
    }

    /** Telemetry to the Driver Station and FTC Dashboard. */
    public Telemetry telem() {
        if(!isTest) return opMode.telem;
        return environment.telem;
    }

    /** A GlobalVariables object, built on first use. */
    public GlobalVariables globalVariables() {
        if(!isTest) throw new IllegalStateException("This is not a test, there is no environment");
        if (environment.globalVariables == null) {
            try {
                environment.globalVariables = new GlobalVariables();
            } catch (Throwable t) {
                throw new UnavailableSubsystem("Global Variables failed to initialize", t);
            }
        }

        return environment.globalVariables;
    }



    /** Shows an instruction to the tester on the "Instruction" telemetry line. */
    public void prompt(String instruction) {
        opMode.telem.addData("Instruction", instruction);
    }

    public Gamepad gamepad1() {
        return opMode.gamepad1;
    }

    public Gamepad gamepad2() {
        return opMode.gamepad2;
    }



    /** @return seconds since this test started */
    public double testSeconds() {
        return timer.seconds();
    }

    /** @return whether more than the given number of seconds have passed since this test started */
    public boolean after(double seconds) {
        return timer.seconds() > seconds;
    }



    // One loop of the test: update the localizer if the test built one, then step the test.
    protected boolean tick() {
        if(isTest) opMode.telem.addData("TEST", test.category + " [" + test.status + "]: " + test.name);
        else opMode.telem.addData("ITEM", "RUNNING");
        if(isTest) if(environment.localizer != null) environment.localizer.run();
        return task.step(this);
    }

    // After the test: its own cleanup, then the routine's hardware cleanup, then close any table files.
    // Errors here are ignored so that the next test still runs.
    protected void close() {
        try { task.cleanup(this); } catch (Throwable ignored) {}
        try { opMode.cleanupHardware(this, environment.hardware != null); }
        catch (Throwable ignored) {}
        try { if(environment.optimalityEngine != null) environment.optimalityEngine.closeReaders(); }
        catch (Throwable ignored) {}
    }
}
