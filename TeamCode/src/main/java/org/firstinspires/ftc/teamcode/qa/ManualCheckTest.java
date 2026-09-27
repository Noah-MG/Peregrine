package org.firstinspires.ftc.teamcode.qa;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.Gamepad;

public class ManualCheckTest extends QATest{

    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    public interface StepSupplier {
        void run(QAContext ctx);
    }

    StepSupplier stepSupplier;
    NoteSupplier noteSupplier;
    String instruction;
    boolean firstCall;
    Gamepad gamepad1;

    public ManualCheckTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, String instruction) {
        super(name, category);
        this.stepSupplier = stepSupplier;
        this.noteSupplier = noteSupplier;
        this.instruction = instruction;
        firstCall = true;
    }

    @Override
    protected boolean step(QAContext ctx) {
        if(firstCall) {
            status = Status.RUNNING;
            ctx.prompt(instruction + "\n Press A for PASS, B for FAIL, X for SKIP, Y for ERROR");
            gamepad1 = ctx.gamepad1();
            firstCall = false;
        }
        stepSupplier.run(ctx);

        if(gamepad1.aWasPressed()) status = Status.PASS;
        if(gamepad1.bWasPressed()) status = Status.FAIL;
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
        return Long.MAX_VALUE;
    }
}
