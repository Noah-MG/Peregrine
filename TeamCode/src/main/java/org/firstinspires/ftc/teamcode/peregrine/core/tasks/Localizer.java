package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class Localizer extends Task {

    public Localizer (PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    public boolean run() {
        return false;
    }

    public boolean end() {
        return true;
    }

    public Task reset() {
        return new Localizer(opMode);
    }

}
