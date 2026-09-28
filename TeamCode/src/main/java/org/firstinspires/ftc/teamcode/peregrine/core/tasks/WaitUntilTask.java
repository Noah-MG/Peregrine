package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

import java.util.function.BooleanSupplier;

/**
 * <h3>Waits until a condition becomes true, then finishes.</h3>
 *
 * <p>The condition is checked once per loop, and is usually written as a lambda, for example
 * {@code new WaitUntilTask(() -> gamepad1.a)} or
 * {@code new WaitUntilTask(() -> hardware.touchSensor.isPressed())}.</p>
 */
@Ends
public class WaitUntilTask extends Task {

    BooleanSupplier condition;

    /**
     * @param condition checked every loop; the task finishes on the first loop it returns true
     */
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
