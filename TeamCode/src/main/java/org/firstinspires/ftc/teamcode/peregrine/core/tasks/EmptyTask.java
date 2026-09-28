package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>A task that does nothing and is immediately finished.</h3>
 *
 * <p>Used by SeriesTask and ParallelTask to fill an empty slot when they are given fewer than two
 * tasks, and by ParallelRaceTask when it is given none. It is also a handy placeholder while you are
 * still writing a routine.</p>
 */
@Ends
public class EmptyTask extends Task {

    public boolean run() {
        return true;
    }

    public void end() {}

    public Task reset() {
        return new EmptyTask();
    }

}
