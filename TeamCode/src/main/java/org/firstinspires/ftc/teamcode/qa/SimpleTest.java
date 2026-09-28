package org.firstinspires.ftc.teamcode.qa;

public class SimpleTest extends QATest {

    public interface NoteSupplier {
        String get(QAContext ctx);
    }

    public interface StepSupplier {
        Status run(QAContext ctx);
    }

    public interface CleanupSupplier {
        void run(QAContext ctx);
    }

    NoteSupplier noteSupplier;
    StepSupplier stepSupplier;
    CleanupSupplier cleanupSupplier;
    long timeout;
    boolean firstCall;

    public SimpleTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, CleanupSupplier cleanupSupplier) {
        this(name, category, noteSupplier, stepSupplier, cleanupSupplier, QATest.defaultTimeout);
    }

    public SimpleTest(String name, Category category, NoteSupplier noteSupplier, StepSupplier stepSupplier, CleanupSupplier cleanupSupplier, long timeout) {
        super(name, category);
        this.noteSupplier = noteSupplier;
        this.stepSupplier = stepSupplier;
        this.cleanupSupplier = cleanupSupplier;
        firstCall = true;
        this.timeout = timeout;
    }

    @Override
    protected boolean step(QAContext ctx) {
        if(firstCall) {
            status = Status.RUNNING;
            firstCall = false;
        }
        Status s = stepSupplier.run(ctx);
        if (s == null || s == Status.PENDING) status = Status.ERROR;
        else if (s != Status.RUNNING) {
            status = s;
            if (noteSupplier != null) note = noteSupplier.get(ctx);
            return true;
        }
        return false;
    }

    @Override
    protected void cleanup(QAContext ctx) {
        if (cleanupSupplier != null) cleanupSupplier.run(ctx);
    }

    @Override
    protected long timeoutMs() {
        return timeout;
    }
}
