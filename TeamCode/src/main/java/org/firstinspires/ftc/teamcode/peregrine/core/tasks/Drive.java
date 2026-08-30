package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Kinematics;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class Drive extends Task {

    PeregrineOpMode opMode;
    String targetName;
    int target;

    public Drive(PeregrineOpMode opMode, String target) {
        this.opMode = opMode;
        targetName = target;
        this.target = opMode.optimalityEngine.targets.get(targetName);
    }

    @Override
    public boolean run() {
        double[] control = opMode.optimalityEngine.solve(target);
        Kinematics.powerMotors(control[0], control[1], control[2], opMode);
        return false; //TODO: make this actually know when it is done and possibly let it hone in on the target at the end with a PID???
    }

    @Override
    public boolean end() {
        return true;
    }

    @Override
    public Task reset() {
        return new Drive(opMode, targetName);
    }
}
