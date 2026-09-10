---
title: Driving to targets
nav_order: 5
---

# Driving to targets
{: .no_toc }

1. TOC
{:toc}

## How it works

The desktop solver computes the **shortest possible time to reach a target** from every state the
robot could be in, meaning every combination of position, heading and velocity. It stores the
results on the SD card as a table, one table per target.

Each loop, the robot:

1. reads its current position, heading and velocity from the Pinpoint;
2. looks up the table cells around that state and works out which direction makes the time to go
   fall fastest;
3. uses the fitted drivetrain model to pick the motor command that moves it that way quickest,
   allowing for wheel slip.

Nothing is planned in advance. If the robot gets bumped off course, it simply continues from
wherever it ended up. Walls and obstacles are already accounted for in the tables.

## Using it

```java
new Drive(this, "score_left")
```

The name has to match a target `name` in the card's `MANIFEST.JSON` exactly. An unknown name crashes
at init.

`Drive` works in three phases:

| Phase | When | What drives the robot |
| --- | --- | --- |
| **Fast** | Far from the target | The minimum-time command from the tables, always at full power. |
| **Honing** | Within `PIDDist` (3 cm) and `PIDAng` (0.5 rad) of the target, **or** when the tables give no usable direction | The honing PID (`PIDHold`), gentle and precise. |
| **Done** | Within `DoneDist` (0.5 cm) and `DoneAng` (0.08 rad) | Motors stop, and `Drive` finishes. |

The tables deliberately stop short of the exact point, because getting the last few centimetres right
is a different problem from getting there fast. The PID handles that part. Its gains come ready-made
in `MODEL.JSON`, so **there is nothing to tune**.

All four thresholds can be changed live from [FTC Dashboard](tuning.md).

## Coordinates

Everything is in the **field frame**: x and y in **centimetres**, heading in **radians**. It is the
same frame the tables were solved in. Your `startingPose()` has to be given in that frame, and it
has to be accurate.

{: .warning }
The tables only cover positions the robot can physically occupy, so they are **inset from the walls**
by the robot's footprint plus a clearance, for example 23 cm on every side. If the robot starts or ends
up outside that area, the tables have nothing to say. `Drive` then falls back to the PID, which
drives in a straight line and ignores obstacles.

## Speed limits

The tables cover a fixed range of velocities, set when they were solved. A robot moving faster than
that range is treated as having no valid route. In practice this only matters if something external
throws the robot around.

## Getting unstuck

If the robot gets pushed into an obstacle, or ends up against a wall at a heading that doesn't fit,
the tables still tell it the quickest way back out. It escapes first and then carries on to the
target.

## Trying it out

`tests/PathingTest.java` is a ready-made autonomous that drives to one target while logging the run to
the SD card. The target name and starting pose (`target`, `x0`, `y0`, `h0`) are editable from FTC
Dashboard.

{: .note }
`PathingTest`'s default starting pose, (1, 1) cm, is outside the table area, so change it before you
run.

## Changing targets or the field

Regenerate the tables on the desktop and swap the SD card. No robot code changes are needed. The
grid size, resolution and target list are all read from the card.
