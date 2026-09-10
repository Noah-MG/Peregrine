---
title: Tuning with Dashboard
nav_order: 8
---

# Tuning with FTC Dashboard
{: .no_toc }

1. TOC
{:toc}

Connect to the robot's Wi-Fi and open **http://192.168.43.1:8080/dash** in a browser. You'll see
telemetry from `opMode.telem`, plus a **Configuration** panel where the values below can be edited
live.

{: .note }
Dashboard edits are lost when the app restarts. Once you've found a value you like, copy it into
the code.

## Driving to targets (`Drive`)

| Parameter | Default | Meaning |
| --- | --- | --- |
| `PIDDist` | 3 cm | Hand over to the honing PID within this distance of the target… |
| `PIDAng` | 0.5 rad | …and within this heading error. |
| `DoneDist` | 0.5 cm | `Drive` finishes within this distance… |
| `DoneAng` | 0.08 rad | …and within this heading error. |

Takes effect immediately.

## The optimizer (`OptimalityEngine`)

| Parameter | Default | Meaning |
| --- | --- | --- |
| `deadZone` | 0.3 | Ignore any drive axis (forward, strafe or turn) that contributes less than this fraction of the strongest one. Higher values give simpler, less twitchy commands. Lower values give more exact ones. |
| `window` | 4 | How many recent commands are averaged to smooth out chatter. Higher is smoother but slower to react. Only takes effect when an opMode is initialised. |
| `iterations` | 6 | Solver iterations per loop. You shouldn't need to change this. |

## Robot constants (`RobotParams`)

Chassis type, odometry offsets and directions, and motor directions. See
[Getting started](getting-started.md#robotparams). These are only read when an opMode is
initialised.

## The pathing test (`PathingTest`)

| Parameter | Default | Meaning |
| --- | --- | --- |
| `target` | `score_left` | Which target to drive to. |
| `x0`, `y0` | 1, 1 cm | Starting position. **Change this.** The default is outside the table area. |
| `h0` | 0.5 rad | Starting heading. |

## What isn't tunable

The **honing PID gains** come from `MODEL.JSON` and are derived from the same drivetrain fit as
everything else. If the robot's final approach is off, re-calibrate rather than tweaking gains.
