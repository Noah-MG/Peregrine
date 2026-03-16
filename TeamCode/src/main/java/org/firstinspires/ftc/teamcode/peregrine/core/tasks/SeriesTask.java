package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>A compound task that plays each input in order</h3>
 * <p>Input a set of tasks into the constructor, and upon running the SeriesTask each of the inputs
 * will be run, in the order in which you inputted them, one after another.</p>
 */

public class SeriesTask extends Task {

    //The first task to be run
    Task taskOne;
    //The second task to be run
    Task taskTwo;

    //Is the first task finished
    boolean taskOneDone;

    /**
     * Initializes the SeriesTask
     * @param tasks a list of the tasks to run, in order
     */
    public SeriesTask(Task... tasks) {
        if (tasks.length > 2) { // If there are more items in the tasks array than a single SeriesTask can take
            taskTwo = tasks[tasks.length-1]; // Place one item from the list into this SeriesTask
            Task[] remainingTasks = new Task[tasks.length-1];
            System.arraycopy(tasks, 0, remainingTasks, 0, tasks.length - 1);
            taskOne = new SeriesTask(remainingTasks); // Create a new SeriesTask for the other items and place it into this one
        } else if (tasks.length == 2) { //Base case: each item is assigned to a task
            taskOne = tasks[0];
            taskTwo = tasks[1];
        } else if (tasks.length == 1) {
            taskOne = new EmptyTask();
            taskTwo = tasks[0];
        } else {
            taskOne = new EmptyTask();
            taskTwo = new EmptyTask();
        }
    }

    public boolean run() {
        if(!taskOneDone){
            taskOneDone = taskOne.run();
        }
        if(taskOneDone) {
            return taskTwo.run();
        }
        return false;
    }

    public boolean end(){
        if(!taskOneDone) {
            return taskOne.end();
        } else {
            return taskTwo.end();
        }
    }

    public Task reset() {
        return new SeriesTask(taskOne.reset(), taskTwo.reset());
    }

}
