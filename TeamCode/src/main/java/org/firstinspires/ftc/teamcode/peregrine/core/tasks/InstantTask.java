package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

@Ends
public class InstantTask extends Task {

    Runnable runnable;

    public InstantTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public boolean run() {
        runnable.run();
        return true;
    }

    @Override
    public void end() {

    }

    @Override
    public Task reset() {
        return new InstantTask(runnable);
    }
}
