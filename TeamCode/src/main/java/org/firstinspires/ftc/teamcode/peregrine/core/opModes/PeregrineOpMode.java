package org.firstinspires.ftc.teamcode.peregrine.core.opModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.peregrine.core.tasks.Localizer;
import org.firstinspires.ftc.teamcode.peregrine.editables.GlobalVariables;
import org.firstinspires.ftc.teamcode.peregrine.editables.Hardware;

/**
 * <h3>PeregrineOpMode is the basic class that all opModes extend from, replacing LinearOpMode in
 * the normal FTC SDK.</h3>
 *
 * <p>PeregrineOpMode should never directly be extended by any of your opModes. It is further extended
 * by PeregrineAutonomous and PeregrineTeleop, which should be used for their respective opMode
 * types instead of PeregrineOpMode. It provides a universal interface that can be used by tasks
 * to access to robot's hardware and any other global variables that you may choose to add to the
 * GlobalVariables class.</p>
 */

public abstract class PeregrineOpMode extends LinearOpMode {

    /**This is the hardware object that contains all maps to robot hardware and that can be accessed through the opMode by any other class*/
    public Hardware hardware;

    /**This is the localizer object that keeps track of the robot's position using various sensors. It is specialized task that all opModes have.*/
    public Localizer localizer;

    /**This is the global variables object that contains every variable you use for your specific robot. It is in the editables package to allow you to add more detail.*/
    public GlobalVariables globalVariables;

    /**
     * This is the telemetry object, it can be used to push telemetry the ftc dashboard on 192.168.43.1:8080/dash on your robot's network
     */
    public Telemetry telem;

    /**
     * This function should return the pose of the robot when init is pressed
     * @return The pose of the robot when init is pressed
     */
    public abstract Pose2D startingPose();

    //This is the regular opMode function, being mapped to those below
    public void runOpMode() {

        telem = FtcDashboard.getInstance().getTelemetry();
        hardware = new Hardware(this);
        localizer = new Localizer(this, startingPose());
        globalVariables = new GlobalVariables();

        initStart();

        while(opModeInInit()) {
            initLoop();
        }

        waitForStart();

        mainStart();

        while(opModeIsActive() && !mainLoop()) {
            telem.update();
            telemetry.update();
        }

        end();

    }

    /**Is run once at the start of init.*/
    public abstract void initStart();

    /**Is run repeatedly throughout init until you press run.*/
    public abstract void initLoop();

    /**Is run once immediately after you press start.*/
    public abstract void mainStart();

    /**Is run repeatedly after the start button is pressed, it is where the main body of code is run.*/
    public abstract boolean mainLoop();

    /**Is run once at the end of the opMode.*/
    public abstract void end();

}
