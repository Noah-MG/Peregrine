package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.util.function.BooleanSupplier;

@Ends
public class WaitUntilTask extends Task {

    BooleanSupplier condition;

    public WaitUntilTask(BooleanSupplier condition) {
        this.condition = condition;
    }

    @Override
    public boolean run() {
        return condition.getAsBoolean();
    }

    @Override
    public void end() {

    }

    @Override
    public Task reset() {
        return new WaitUntilTask(condition);
    }
}
