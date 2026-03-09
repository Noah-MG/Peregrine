package org.firstinspires.ftc.teamcode.peregrine.core;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.peregrine.editables.GlobalVariables;
import org.firstinspires.ftc.teamcode.peregrine.editables.Hardware;

/**
 * <h3>PeregrineOpMode is the basic class that all opModes extend from, replacing LinearOpMode in
 * the normal FTC SDK.</h3>
 *
 * <p>PeregrineOpMode should never directly be mentioned by any of your code. It is further extended
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

    //This is the regular opMode function, being mapped to those below
    public void runOpMode() {

        initStart();

        while(opModeInInit()) {
            initLoop();
        }

        waitForStart();

        mainStart();

        while(opModeIsActive()) {
            mainLoop();
        }

        end();

    }

    /**This gets run once at the start of init.*/
    public abstract void initStart();

    /**This gets run repeatedly throughout init until you press run.*/
    public abstract void initLoop();

    /**This gets run once immediately after you press start.*/
    public abstract void mainStart();

    /**This gets run repeatedly after the start button is pressed, it is where the main body of code is run.*/
    public abstract void mainLoop();

    /**This gets run once at the end of the opMode.*/
    public abstract void end();

}
