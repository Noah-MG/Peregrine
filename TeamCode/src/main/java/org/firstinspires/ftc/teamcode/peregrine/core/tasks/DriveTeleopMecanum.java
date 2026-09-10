package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Placeholder for a mecanum teleop drive task.</h3>
 *
 * <p>Not implemented yet: run() does nothing and never finishes. TeleopMovement is the working
 * teleop drive task.</p>
 */
public class DriveTeleopMecanum extends Task {

    public DriveTeleopMecanum(PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    public boolean run() {
        return false;
    }

    public boolean end() {
        return true;
    }

    public Task reset() {
        return new DriveTeleopMecanum(opMode);
    }

}
