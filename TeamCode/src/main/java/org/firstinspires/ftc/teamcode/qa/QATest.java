package org.firstinspires.ftc.teamcode.qa;

import androidx.annotation.Nullable;

public abstract class QATest {
    public enum Category { SOFTWARE, HARDWARE, INTEGRATED }
    public enum Status { PENDING, RUNNING, PASS, FAIL, SKIP, ERROR }

    public final String name;
    public final Category category;
    Status status = Status.PENDING;
    @Nullable public String note;
    public static final long defaultTimeout = 15000;

    public QATest(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected abstract boolean step(QAContext ctx);
    protected void cleanup(QAContext ctx) {}
    protected long timeoutMs() { return defaultTimeout; }
}

