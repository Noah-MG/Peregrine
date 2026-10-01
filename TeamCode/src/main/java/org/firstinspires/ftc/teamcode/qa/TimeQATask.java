package org.firstinspires.ftc.teamcode.qa;

import com.qualcomm.robotcore.util.ElapsedTime;

public class TimeQATask implements QATask {
    public interface StringSupplier {
        String getAsString(QAContext ctx);
    }

    StringSupplier stringSupplier;
    long timeMs;

    public TimeQATask(long timeMs, StringSupplier stringSupplier) {
        this.timeMs = timeMs;
        this.stringSupplier = stringSupplier;
    }

    @Override
    public boolean step(QAContext ctx) {
        if(stringSupplier != null) ctx.prompt(stringSupplier.getAsString(ctx));
        return ctx.testSeconds() * 1000 > timeMs;
    }
}
