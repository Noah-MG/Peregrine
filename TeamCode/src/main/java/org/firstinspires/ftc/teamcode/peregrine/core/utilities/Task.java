package org.firstinspires.ftc.teamcode.peregrine.core.utilities;

import org.firstinspires.ftc.teamcode.peregrine.core.annotations.Ends;

/**
 *  <h3>The class that represents any action the robot might take, no matter how complicated</h3>
 *
 *  <p>Examples of this include: changing a servo position, moving across the field, or running an
 *  entire autonomous. This is achieved by running the run() function in a loop.</p>
 *
 *  <p>Tasks are composed into trees with SeriesTask, ParallelTask and ParallelRaceTask. The root is
 *  ticked once per loop by PeregrineOpMode.</p>
 */

public abstract class Task {

    /**
     * Stores the opMode itself, from which most references to things running independently of this
     * task derive.
     */
    protected PeregrineOpMode opMode;

    /**
     * Executes the task. Should be run in a loop until it returns true.
     *
     * @return Whether the task has finished. Unending tasks always return false.
     */
    public abstract boolean run();

    /**
     * Puts the task into a safe end state if it must be ended early, for example at the end of a
     * ParallelRaceTask
     * @return Whether the task has finished ending.
     */
    public abstract boolean end();

    /**
     * Returns a fresh copy of the task, resetting all changes.
     *
     * @return a fresh copy of the task
     */
    public abstract Task reset();

    /**
     * Returns whether this task ends, evaluate the children of any subtasks if compound
     *
     * @return Whether this task ends
     */
    public boolean ends() {
        return this.getClass().isAnnotationPresent(Ends.class);
    }
}
