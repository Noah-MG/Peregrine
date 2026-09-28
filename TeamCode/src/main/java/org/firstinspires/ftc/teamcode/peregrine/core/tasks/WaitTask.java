package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>Waits for a fixed amount of time, then finishes.</h3>
 *
 * <p>The clock starts on the task's first run(), not when it is built, so a WaitTask placed later in
 * a SeriesTask waits after the tasks before it have finished.</p>
 */
@Ends
public class WaitTask extends Task {

    // How long to wait, in milliseconds.
    long time;
    // Started on the first run().
    ElapsedTime elapsedTime;

    /**
     * @param opMode the running opMode
     * @param timeMs how long to wait, in milliseconds
     */
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