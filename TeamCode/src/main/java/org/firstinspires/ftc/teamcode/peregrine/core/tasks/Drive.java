package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.peregrine.core.opModes.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.TableReader;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

public class Drive extends Task {

    PeregrineOpMode opMode;
    TableReader tableReader;
    ElapsedTime cycle;

    double[] gradient;
    String output;

    public Drive(PeregrineOpMode opMode) {
        this.opMode = opMode;
        tableReader = new TableReader(opMode);
        cycle = new ElapsedTime();
    }

    @Override
    public boolean run() {
//        gradient = tableReader.getGradient(1, new double[]{50, 45, 2.738, 0.63, 0.01, 0});
        output = gradient[0] + ", " + gradient[1] + ", " + gradient[2] + ", " + gradient[3] + ", " + gradient[4] + ", " + gradient[5];
        opMode.telem.addData("gradient", output);
        opMode.telem.addData("cycle time", cycle.milliseconds());
        cycle.reset();
        return true;
    }

    @Override
    public boolean end() {
        tableReader.closeReaders();
        return true;
    }

    @Override
    public Task reset() {
        return new Drive(opMode);
    }
}
