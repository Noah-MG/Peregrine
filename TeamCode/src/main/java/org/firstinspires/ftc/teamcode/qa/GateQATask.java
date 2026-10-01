package org.firstinspires.ftc.teamcode.qa;

public class GateQATask implements QATask {
    public interface StringSupplier {
        String getAsString(QAContext ctx);
    }

    public interface BooleanSupplier {
        boolean getAsBoolean(QAContext ctx);
    }

    BooleanSupplier condition;
    StringSupplier instruction;

    public GateQATask(BooleanSupplier condition, StringSupplier instruction) {
        this.condition = condition;
        this.instruction = instruction;
    }

    @Override
    public boolean step(QAContext ctx) {
        if(instruction != null) ctx.prompt(instruction.getAsString(ctx));
        return condition.getAsBoolean(ctx);
    }
}
