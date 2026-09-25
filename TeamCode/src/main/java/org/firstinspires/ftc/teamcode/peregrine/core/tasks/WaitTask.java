package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@Ends
public class WaitTask extends Task {

    long time;
    ElapsedTime elapsedTime;

    public WaitTask(PeregrineOpMode opMode, long timeMs) {
        this.opMode = opMode;
        time = timeMs;
    }

    @Override
    public boolean run() {
        if (elapsedTime == null) elapsedTime = new ElapsedTime();
        return elapsedTime.milliseconds() > time;
    }

    @Override
    public void end() {}

    @Override
    public Task reset() {
        return new WaitTask(opMode, time);
    }
}