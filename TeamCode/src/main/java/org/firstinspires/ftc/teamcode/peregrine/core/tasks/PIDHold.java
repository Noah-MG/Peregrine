package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import static androidx.core.math.MathUtils.clamp;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.ejml.simple.SimpleMatrix;
import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Kinematics;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class PIDHold extends Task {

    PeregrineOpMode opMode;
    String targetName;
    int target;
    SimpleMatrix targetState;

    SimpleMatrix Kp;
    SimpleMatrix Ki;
    SimpleMatrix Kd;
    SimpleMatrix bEffInv;
    SimpleMatrix lambda;
    SimpleMatrix integralLimit;

    SimpleMatrix p;
    SimpleMatrix i;
    SimpleMatrix d;

    SimpleMatrix state;
    SimpleMatrix e;

    SimpleMatrix R;

    SimpleMatrix u;

    ElapsedTime dt;

    public PIDHold(PeregrineOpMode opMode, String target) {
        this.opMode = opMode;
        targetName = target;
        this.target = opMode.optimalityEngine.targets.get(targetName);
        targetState = new SimpleMatrix(new double[][]{opMode.optimalityEngine.getTargetCoords(this.target)}).transpose();

        SimpleMatrix[] pidConstants = opMode.optimalityEngine.getPIDConstants();
        Kp = pidConstants[0];
        Ki = pidConstants[1];
        Kd = pidConstants[2];
        bEffInv = pidConstants[3];
        lambda = pidConstants[4];
        integralLimit = pidConstants[5];

        p = new SimpleMatrix(3, 1);
        i = new SimpleMatrix(3, 1);
        d = new SimpleMatrix(3, 1);

        state = new SimpleMatrix(6, 1);
        e = new SimpleMatrix(6, 1);

        R = new SimpleMatrix(6, 6);

        u = new SimpleMatrix(3, 1);
    }

    @Override
    public boolean run() {
        if (dt == null) dt = new ElapsedTime();
        state = new SimpleMatrix(new double[][]{opMode.localizer.getStateVector()}).transpose();
        R = new SimpleMatrix(new double[][]{
                {Math.cos(-state.get(2)), -Math.sin(-state.get(2)), 0, 0, 0, 0},
                {Math.sin(-state.get(2)), Math.cos(-state.get(2)), 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0},
                {0, 0, 0, Math.cos(-state.get(2)), -Math.sin(-state.get(2)), 0},
                {0, 0, 0, Math.sin(-state.get(2)), Math.cos(-state.get(2)), 0},
                {0, 0, 0, 0, 0, 1}
        });
        e = R.mult(targetState.minus(state));
        e.set(2, mod(e.get(2) + Math.PI, 2*Math.PI) - Math.PI);
        if(Math.abs(u.get(0)) + Math.abs(u.get(1)) + Math.abs(u.get(2)) < 1 - 1e-12) {
            for (int x = 0; x < 3; x++) {
                i.set(x,
                        clamp(i.get(x) + e.get(x) * dt.seconds(),
                                -integralLimit.get(x), integralLimit.get(x)));
            }
        }
        u = Kp.mult(e.rows(0, 3)).plus(Ki.mult(i)).plus(Kd.mult(e.rows(3, 6)));
        Kinematics.powerMotors(u.get(0), u.get(1), u.get(2), opMode);
        dt.reset();
        return false;
    }

    @Override
    public boolean end() {
        Kinematics.powerMotors(0, 0, 0, opMode);
        return true;
    }

    @Override
    public Task reset() {
        return new PIDHold(opMode, targetName);
    }

    private double mod(double a, double b) {
        return ((a % b) + b) % b;
    }
}
