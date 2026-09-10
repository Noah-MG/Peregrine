---
title: Tasks
nav_order: 4
---

# Tasks
{: .no_toc }

1. TOC
{:toc}

Everything the robot does is a **task**. Examples include moving a servo, driving to a target, or
running a whole autonomous routine. Tasks are built into a tree and the tree is run once per loop.
Nothing ever blocks, so many things can happen at once.

## The three methods

Every task extends `Task` and implements:

| Method | Contract |
| --- | --- |
| `boolean run()` | Called once per loop. Do a little work and return quickly. Return **`true` when the task is finished**, `false` to keep going. Tasks that never end always return `false`. |
| `boolean end()` | Called if the task has to stop early, for example when it loses a `ParallelRaceTask` or the opMode stops. Put the robot in a safe state, such as motors off. Return `true` once it has finished ending. |
| `Task reset()` | Return a brand-new copy of the task, as if it had never run. |

{: .warning }
Never `sleep()` or loop inside `run()`. The whole robot shares one loop, so a task that blocks
freezes odometry and every other task.

## Built-in tasks

### Combining tasks

| Task | Runs its children… | Finishes when… |
| --- | --- | --- |
| `SeriesTask(a, b, c, …)` | one at a time, in order | the last one finishes |
| `ParallelTask(a, b, c, …)` | all at once | **all** of them have finished |
| `ParallelRaceTask(a, b, c, …)` | all at once | **any one** of them finishes |
| `EmptyTask()` | nothing | immediately |

All of these accept any number of tasks, and they nest freely:

```java
return new SeriesTask(
        new Drive(this, "pickup"),
        new ParallelTask(
                new Drive(this, "score_left"),
                new RaiseLift(this)          // your own task
        ),
        new Drive(this, "park")
);
```

### Driving

| Task | What it does |
| --- | --- |
| `Drive(opMode, "target")` | Drives to a named target from the SD card tables, then finishes. See [Driving to targets](driving-to-targets.md). |
| `PIDHold(opMode, "target")` | Holds the robot on a target with the honing PID. Never finishes on its own. `Drive` uses it internally. |
| `TeleopMovement(opMode)` | Robot-centric mecanum driving from gamepad 1. Never finishes. |

## Writing your own task

A task that waits a number of seconds:

```java
public class Wait extends Task {

    private final double seconds;
    private ElapsedTime timer;

    public Wait(PeregrineOpMode opMode, double seconds) {
        this.opMode = opMode;   // inherited from Task
        this.seconds = seconds;
    }

    @Override
    public boolean run() {
        // Start the timer on the first run, not in the constructor: tasks are built during init.
        if (timer == null) timer = new ElapsedTime();
        return timer.seconds() >= seconds;
    }

    @Override
    public boolean end() {
        return true;            // nothing to clean up
    }

    @Override
    public Task reset() {
        return new Wait(opMode, seconds);
    }
}
```

A task that moves a servo and finishes straight away:

```java
public class SetClaw extends Task {

    private final double position;

    public SetClaw(PeregrineOpMode opMode, double position) {
        this.opMode = opMode;
        this.position = position;
    }

    @Override
    public boolean run() {
        opMode.hardware.claw.setPosition(position);   // add `claw` to Hardware.java first
        return true;
    }

    @Override public boolean end() { return true; }
    @Override public Task reset() { return new SetClaw(opMode, position); }
}
```

## Things to know

- **Constructors run during init.** Start timers and read sensors in `run()`, not in the
  constructor.
- **`run()` can be called again after it returns `true`.** `ParallelTask` keeps running finished
  children until all of them are done. Make sure a second call is harmless.
- **In a `SeriesTask`, the next task starts on the same loop the previous one finished.**
- **`ParallelRaceTask` doesn't automatically `end()` the tasks that lost the race.** If a loser needs
  cleaning up, make sure its `end()` gets called, or follow the race with a task that does the cleanup.
