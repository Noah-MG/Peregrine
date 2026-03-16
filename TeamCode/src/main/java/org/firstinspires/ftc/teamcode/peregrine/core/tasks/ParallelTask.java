package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>A compound task that plays each input simultaneously</h3>
 * <p>Input a set of tasks into the constructor, and upon running the ParallelTask, all of the
 * inputs will be played simultaneously. The task will have completed once all of its components
 * have completed.</p>
 */

public class ParallelTask extends Task {

    //One of the tasks to be run
    Task taskOne;
    //Another of the tasks to be run
    Task taskTwo;
    //Is the first task finished
    boolean taskOneDone;
    //Is the second task finished
    boolean taskTwoDone;

    /**
     * Initializes the ParallelTask
     * @param tasks the list of the tasks to run
     */
    public ParallelTask(Task... tasks) {
        if (tasks.length > 2) { // If there are more items in the tasks array than a single ParallelTask can take
            taskTwo = tasks[tasks.length-1]; // Place one item from the list into this ParallelTask
            Task[] remainingTasks = new Task[tasks.length-1];
            System.arraycopy(tasks, 0, remainingTasks, 0, tasks.length - 1);
            taskOne = new ParallelTask(remainingTasks); // Create a new ParallelTask for the other items and place it into this one
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
        taskOneDone = taskOne.run();
        taskTwoDone = taskTwo.run();
        return taskOneDone || taskTwoDone;
    }

    public boolean end() {
        return taskOne.end() && taskTwo.end();
    }

    public Task reset() {
        return new ParallelTask(taskOne.reset(), taskTwo.reset());
    }

}
