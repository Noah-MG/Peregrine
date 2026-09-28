package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>A task that does nothing and never finishes.</h3>
 *
 * <p>Used by ParallelRaceTask to fill the empty slot when it is given a single task, so the race
 * finishes exactly when that task does and ends() reports whatever that task reports. It has no
 * {@code @Ends} annotation, so ends() returns false. The opposite of EmptyTask, which finishes
 * immediately.</p>
 *
 * <p>It is also useful to keep a tree running until the opMode is stopped, for example
 * {@code new ParallelTask(myRoutine, new IdleTask())} keeps the opMode alive after myRoutine is done.</p>
 */
public class IdleTask extends Task {

    public IdleTask() {

    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    public void end() {

    }

    @Override
    public Task reset() {
        return new IdleTask();
    }
}
