package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Kinematics;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.util.Arrays;

public class Drive extends Task {

    PeregrineOpMode opMode;
    String targetName;
    int target;
    double[] targetState;
    double distSq;

    public static double PIDDist = 3;
    public static double PIDAng = 0.5;

    public static double DoneDist = 0.5;
    public static double DoneAng = 0.08;

    Task pidHold;

    public Drive(PeregrineOpMode opMode, String target) {
        this.opMode = opMode;
        targetName = target;
        this.target = opMode.optimalityEngine.targets.get(targetName);
        targetState = opMode.optimalityEngine.getTargetCoords(this.target);

        pidHold = new PIDHold(opMode, target);
    }

    @Override
    public boolean run() {
        double[] control = opMode.optimalityEngine.solve(target);
        distSq = Math.pow(opMode.localizer.getPose().getX(DistanceUnit.CM) - targetState[0], 2)
               + Math.pow(opMode.localizer.getPose().getY(DistanceUnit.CM) - targetState[1], 2);
        if (distSq < Math.pow(DoneDist, 2) &&
                Math.abs(opMode.localizer.getPose().getHeading(AngleUnit.RADIANS) - targetState[2]) < DoneAng){
            Kinematics.powerMotors(0, 0, 0, opMode);
            return true;
        } else if (Arrays.equals(control, new double[]{0, 0, 0}) ||
                (distSq < Math.pow(PIDDist, 2) &&
                        Math.abs(opMode.localizer.getPose().getHeading(AngleUnit.RADIANS) - targetState[2]) < PIDAng)) {
            pidHold.run();
        }
        Kinematics.powerMotors(control[0], control[1], control[2], opMode);
        return false;
    }

    @Override
    public boolean end() {
        Kinematics.powerMotors(0, 0, 0, opMode);
        return true;
    }

    @Override
    public Task reset() {
        return new Drive(opMode, targetName);
    }
}
