package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import com.acmerobotics.dashboard.config.Config;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Kinematics;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.util.Arrays;

/**
 * <h3>Drives to a named target from the SD card tables, in three phases.</h3>
 *
 * <ol>
 *     <li><b>Optimal:</b> far from the target, follow the minimum-time command from
 *     OptimalityEngine.solve().</li>
 *     <li><b>Honing:</b> within {@link #PIDDist} cm and {@link #PIDAng} rad, or whenever the optimizer
 *     returns an all-zero command (flat or unreachable neighbourhood), hand over to the honing PID
 *     ({@link PIDHold}). The tables stop at a handoff region rather than at the point itself
 *     (TABLE_FORMAT.MD §8.8 and §9, "The tables hand over; they do not arrive").</li>
 *     <li><b>Done:</b> within {@link #DoneDist} cm and {@link #DoneAng} rad, stop the motors and return true.</li>
 * </ol>
 *
 * <p>Only position and heading are checked; the target velocity is ignored.</p>
 */
@Config
public class Drive extends Task {

    String targetName;
    // Target index into the OptimalityEngine tables.
    int target;
    // Target state [x, y, h, vx, vy, w] in cm / rad, field frame.
    double[] targetState;
    // Squared x/y distance to the target, cm^2.
    double distSq;

    // Honing handoff thresholds (cm, rad). NOTE: MODEL.JSON carries its own honing.handoff
    // (15 cm / 0.25 rad in the TABLE_FORMAT.MD §8.8.6 example), and these do not read it.
    public static double PIDDist = 3;
    public static double PIDAng = 0.5;

    // "Arrived" thresholds (cm, rad).
    public static double DoneDist = 0.5;
    public static double DoneAng = 0.08;

    Task pidHold;

    /**
     * @param target the target's name as written in MANIFEST.JSON, e.g. "score_left".
     */
    public Drive(PeregrineOpMode opMode, String target) {
        this.opMode = opMode;
        targetName = target;
        this.target = opMode.optimalityEngine.targets.get(targetName);
        targetState = opMode.optimalityEngine.getTargetCoords(this.target);

        pidHold = new PIDHold(opMode, target);
    }

    @Override
    public boolean run() {
        // Solved every loop, even in the honing/done phases. This also keeps the boxcar history current.
        double[] control = opMode.optimalityEngine.solve(target);
        distSq = Math.pow(opMode.localizer.getPose().getX(DistanceUnit.CM) - targetState[0], 2)
               + Math.pow(opMode.localizer.getPose().getY(DistanceUnit.CM) - targetState[1], 2);
        if (distSq < Math.pow(DoneDist, 2) &&
                mod(Math.abs(opMode.localizer.getPose().getHeading(AngleUnit.RADIANS) - targetState[2]), 2*Math.PI) < DoneAng){
            // Phase 3: arrived.
            Kinematics.powerMotors(0, 0, 0, opMode);
            return true;
        } else if (Arrays.equals(control, new double[]{0, 0, 0}) ||
                (distSq < Math.pow(PIDDist, 2) &&
                        mod(Math.abs(opMode.localizer.getPose().getHeading(AngleUnit.RADIANS) - targetState[2]), 2*Math.PI) < PIDAng)) {
            // Phase 2: honing PID. Also the fallback whenever the table gives no usable gradient.
            pidHold.run();
            return false;
        }
        // Phase 1: minimum-time command, already [fwd, strafe, turn] with |fwd| + |strafe| + |turn| = 1.
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

    // Always-non-negative modulo. Java's % keeps the sign of the dividend.
    private double mod(double a, double b) {
        return ((a % b) + b) % b;
    }
}
