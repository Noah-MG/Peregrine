package org.firstinspires.ftc.teamcode.qa;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

public class OperatorActionTest extends QATest{

    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    public interface StepSupplier {
        boolean run(QAContext ctx);
    }

    NoteSupplier noteSupplier;
    StepSupplier stepSupplier;
    long timeoutMs;
    boolean firstCall;
    ElapsedTime timer;
    String instruction;
    Gamepad gamepad1;

    public OperatorActionTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, String instruction, long timeoutMs) {
        super(name, category);
        this.noteSupplier = noteSupplier;
        this.stepSupplier = stepSupplier;
        this.instruction = instruction;
        this.timeoutMs = timeoutMs;
        firstCall = true;
    }

    @Override
    protected boolean step(QAContext ctx) {
        if(firstCall) {
            timer = new ElapsedTime();
            status = Status.RUNNING;
            ctx.prompt(instruction + "\n Press X for SKIP, Y for ERROR");
            gamepad1 = ctx.gamepad1();
            firstCall = false;
        }
        if(stepSupplier.run(ctx)) status = Status.PASS;
        if(timer.milliseconds() > timeoutMs) status = Status.FAIL;
        if(gamepad1.xWasPressed()) status = Status.SKIP;
        if(gamepad1.yWasPressed()) status = Status.ERROR;
        if(status != Status.RUNNING) {
            note = noteSupplier.get(ctx);
            return true;
        }
        return false;
    }

    @Override
    protected long timeoutMs() {
        return timeoutMs + 1000;
    }
}
