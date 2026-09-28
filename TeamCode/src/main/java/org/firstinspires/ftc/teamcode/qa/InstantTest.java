package org.firstinspires.ftc.teamcode.qa;


import androidx.annotation.Nullable;

public class InstantTest extends QATest {

    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    public interface StepSupplier {
        Status run(QAContext ctx);
    }

    NoteSupplier noteSupplier;
    StepSupplier stepSupplier;

    public InstantTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier) {
        super(name, category);
        this.noteSupplier = noteSupplier;
        this.stepSupplier = stepSupplier;
    }

    @Override
    protected boolean step(QAContext ctx) {
        status = Status.RUNNING;
        status = stepSupplier.run(ctx);
        note = noteSupplier.get(ctx);
        return true;
    }
}
