package org.firstinspires.ftc.teamcode.peregrine.core.calibration;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class CalibrationMovement extends Task {

    PeregrineOpMode opMode;

    public CalibrationMovement(PeregrineOpMode opMode) {
        this.opMode = opMode;
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public Task reset() {
        return null;
    }
}
