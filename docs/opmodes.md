---
title: Writing opModes
nav_order: 3
---

# Writing opModes
{: .no_toc }

1. TOC
{:toc}

Peregrine replaces `LinearOpMode` with two base classes:

- `PeregrineAutonomous` for autonomous
- `PeregrineTeleop` for teleop

Extend one of them and add the usual `@Autonomous` or `@TeleOp` annotation. Don't extend
`PeregrineOpMode` directly.

## A minimal autonomous

```java
@Autonomous
public class MyAuto extends PeregrineAutonomous {

    // Where the robot is when you press INIT. Field frame, same as the tables.
    @Override
    public Pose2D startingPose() {
        return new Pose2D(DistanceUnit.CM, 60, 60, AngleUnit.RADIANS, 0);
    }

    // The whole routine, as one task tree.
    @Override
    public Task defineTasks() {
        return new SeriesTask(
                new Drive(this, "score_left"),
                new Drive(this, "park")
        );
    }

    @Override public void initLoop() {}
    @Override public void mainStart() {}
    @Override public void finish() {}
}
```

## A minimal teleop

```java
@TeleOp
public class MyTeleop extends PeregrineTeleop {

    @Override
    public Pose2D startingPose() {
        return new Pose2D(DistanceUnit.CM, 0, 0, AngleUnit.RADIANS, 0);
    }

    @Override
    public Task defineTasks() {
        return new TeleopMovement(this);
    }

    @Override public void initLoop() {}
    @Override public void mainStart() {}
    @Override public void finish() {}
}
```

{: .note }
`TeleopMovement` currently sends the left stick's x axis to *forward* and its y axis to *strafe*.
Expect that mapping to change.

## The methods you implement

| Method | When it runs | Use it for |
| --- | --- | --- |
| `startingPose()` | Once, at INIT | Returning where the robot starts. **Accuracy matters**, because everything after this is relative to it. |
| `defineTasks()` | Once, at INIT | Building and returning your task tree. |
| `initLoop()` | Repeatedly, until START | Anything you want to do while waiting, such as reading a camera. |
| `mainStart()` | Once, at START | One-off setup at the moment the match begins. |
| `finish()` | Once, after STOP | Cleanup. |

## What happens, in order

| Stage | What Peregrine does |
| --- | --- |
| **INIT pressed** | Maps the hardware using `RobotParams`. Resets the Pinpoint and waits for it to report ready, then sets its pose to `startingPose()`. Loads the SD card tables. Calls your `defineTasks()`. |
| **During init** | Calls `initLoop()` repeatedly. |
| **START pressed** | Calls `mainStart()`. |
| **Every loop** | Runs the localizer and your task tree once each, then sends telemetry. |
| **STOP pressed** | Closes the table files, calls `end()` on the task tree, then calls your `finish()`. |

{: .warning }
`defineTasks()` runs during **init**, not at start, so your tasks' constructors run then too.
Anything that should begin at START, such as a timer, belongs in the task's first `run()`. See
[Tasks](tasks.md).

## What tasks can reach through `opMode`

Every task gets the opMode it belongs to, and through it:

| Field | What it is |
| --- | --- |
| `opMode.hardware` | Your motors and sensors, from `editables/Hardware.java`. |
| `opMode.localizer` | Current pose and velocity. See `getPose()`, `getVelX()`, `getVelY()`, `getHeadingVelocity()`, `getStateVector()`. |
| `opMode.optimalityEngine` | The pathing tables. You normally only use this through `Drive`. |
| `opMode.globalVariables` | Your own shared state, from `editables/GlobalVariables.java`. |
| `opMode.telem` | Telemetry to FTC Dashboard. It is flushed once per loop for you. |
| `opMode.gamepad1`, `opMode.gamepad2` | The usual gamepads. |

## When does the opMode end?

When you press stop, or when the Driver Station's timer ends it. The localizer runs alongside your
tasks and never finishes, so the opMode keeps running after your routine is done. The robot just
sits still.
