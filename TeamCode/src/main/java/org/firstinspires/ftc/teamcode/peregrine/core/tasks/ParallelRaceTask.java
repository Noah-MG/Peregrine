package org.firstinspires.ftc.teamcode.peregrine.core.tasks;

import org.firstinspires.ftc.teamcode.peregrine.core.utilities.CompoundTask;
import org.firstinspires.ftc.teamcode.peregrine.core.utilities.Task;

/**
 * <h3>A compound task that plays each input simultaneously</h3>
 * <p>Input a set of tasks into the constructor, and upon running the ParallelRaceTask, all of the
 * inputs will be played simultaneously. The task will have completed once one of its components
 * has completed.</p>
 *
 * <p>When the race finishes, every task that hasn't finished yet (the "losers") has end() called on
 * it, so it can stop its motors. A race is a good way to give a task a time limit, for example
 * {@code new ParallelRaceTask(new Drive(this, "park"), new WaitTask(this, 3000))}.</p>
 *
 * <p>Internally it is a binary tree: more than two tasks become a nested ParallelRaceTask plus the
 * last task.</p>
 */

public class ParallelRaceTask extends CompoundTask {

    //One of the tasks to be run
    Task taskOne;
    //Another of the tasks to be run
    Task taskTwo;
    //Is the first task finished
    boolean taskOneDone;
    //Is the second task finished
    boolean taskTwoDone;

    /**
     * Initializes the ParallelRaceTask
     * @param tasks the list of the tasks to run
     */
    public ParallelRaceTask(Task... tasks) {
        if (tasks.length > 2) { // If there are more items in the tasks array than a single ParallelRaceTask can take
            taskTwo = tasks[tasks.length-1]; // Place one item from the list into this ParallelRaceTask
            Task[] remainingTasks = new Task[tasks.length-1];
            System.arraycopy(tasks, 0, remainingTasks, 0, tasks.length - 1);
            taskOne = new ParallelRaceTask(remainingTasks); // Create a new ParallelRaceTask for the other items and place it into this one
        } else if (tasks.length == 2) { //Base case: each item is assigned to a task
            taskOne = tasks[0];
            taskTwo = tasks[1];
        } else if (tasks.length == 1) {
            // Paired with a task that never finishes, so a single-task race finishes when that task does.
            taskOne = new IdleTask();
            taskTwo = tasks[0];
        } else {
            taskOne = new EmptyTask();
            taskTwo = new EmptyTask();
        }
    }

    // Ticks both children every loop and finishes as soon as either one reports done, ending the other.
    public boolean run() {
        taskOneDone = taskOne.run();
        taskTwoDone = taskTwo.run();
        if(taskOneDone || taskTwoDone) {
            if (!taskOneDone) taskOne.end();
            if (!taskTwoDone) taskTwo.end();
            return true;
        }
        return false;
    }

    // Ends both children, unless the race already finished (the losers were ended then).
    public void end() {
        if (taskOneDone || taskTwoDone) return;
        taskOne.end();
        taskTwo.end();
    }

    public Task reset() {
        return new ParallelRaceTask(taskOne.reset(), taskTwo.reset());
    }

    // A race ends if either child is guaranteed to.
    @Override
    public boolean ends() {
        return taskOne.ends() || taskTwo.ends();
    }
}
