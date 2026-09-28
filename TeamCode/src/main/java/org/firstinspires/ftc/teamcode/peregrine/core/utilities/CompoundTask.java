package org.firstinspires.ftc.teamcode.peregrine.core.utilities;

/**
 * <h3>Base class for tasks that are built out of other tasks.</h3>
 *
 * <p>SeriesTask, ParallelTask and ParallelRaceTask extend this. Whether a compound task finishes
 * depends on its children, so instead of using the {@code @Ends} annotation, every subclass must
 * override {@link #ends()} to work it out from them.</p>
 */
public abstract class CompoundTask extends Task {
    @Override
    public abstract boolean ends();
}
