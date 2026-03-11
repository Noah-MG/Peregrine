package org.firstinspires.ftc.teamcode.peregrine.core;

public class EmptyTask extends Task {

    public boolean run() {
        return true;
    }

    public boolean end() {
        return true;
    }

    public Task reset() {
        return new EmptyTask();
    }

}
