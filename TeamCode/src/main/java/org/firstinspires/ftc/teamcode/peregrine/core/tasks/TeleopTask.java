package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.PeregrineOpMode;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.util.function.BooleanSupplier;

public class TeleopTask extends Task {

    Task task;
    BooleanSupplier isPushed;
    boolean togglable;
    boolean toggled;

    boolean isPushedCurr;
    boolean isPushedPrev;

    public TeleopTask(PeregrineOpMode opMode, Task task, BooleanSupplier isPushed, boolean togglable) {
        this.opMode = opMode;
        this.task = task;
        this.isPushed = isPushed;
        this.togglable = togglable;
        toggled = false;
        isPushedCurr = false;
        isPushedPrev = false;
        if (togglable && !task.ends()) {
            throw new IllegalArgumentException("Invalid task, for togglable TeleopTasks ensure the selected task can end.");
        }
    }

    @Override
    public boolean run() {
        isPushedCurr = isPushed.getAsBoolean();
        if (!togglable) {
            if (isPushedCurr && !isPushedPrev) toggled = true;
            if (toggled) if(task.run()) { toggled = false; task = task.reset(); }
            if (isPushedPrev && !isPushedCurr && toggled) { toggled = false; task.end(); task = task.reset(); }
        } else {
            if (isPushedCurr && !isPushedPrev) toggled = true;
            if (toggled) if(task.run()) { toggled = false; task = task.reset(); }
        }
        isPushedPrev = isPushedCurr;
        return false;
    }

    @Override
    public void end() {
        if (toggled) if (!task.run()) task.end();
    }

    @Override
    public Task reset() {
        return new TeleopTask(opMode, task.reset(), isPushed, togglable);
    }
}
