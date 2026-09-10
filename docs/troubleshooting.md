---
title: Troubleshooting
nav_order: 9
---

# Troubleshooting
{: .no_toc }

1. TOC
{:toc}

## Telemetry messages

Most problems show up as a message on FTC Dashboard telemetry, followed by the opMode stopping.

### Loading the tables (any opMode, at INIT)

| Message | What it means | Fix |
| --- | --- | --- |
| `No SD Card found` | The Control Hub can't see a removable card. | Check the card is inserted, then power-cycle. |
| `Tables not found on …` | `MANIFEST.JSON` or `MODEL.JSON` is missing from the card root. | Copy the solver output to the **root** of the card, not into a folder. |
| `SD Card not mounted, did you format it to FAT32?` | The card is there but unreadable. | Reformat as FAT32 and copy the tables again. |
| `Error: …` | A JSON file couldn't be parsed. | Re-copy the files; run `verify_tables.py`. |
| `Please make sure that the SD card tables are generated in the field frame` | The tables were solved in the wrong frame. | Re-solve in the field frame. |
| `SD card table uses unsupported datatype …` / `… should be N bytes …` | The manifest's encoding is invalid. | Re-solve with `u16` (the default). |
| `Could not find table binary on SD card at …` | A `TABLES/*.BIN` chunk is missing. | Re-copy the `TABLES` folder; run `verify_tables.py`. |

### While driving

| Message | What it means | Fix |
| --- | --- | --- |
| `A value from the pathing table could not be retrieved` | A read from the card failed mid-run. | Check the card is seated. Try another card. |

### Calibration logging

| Message | What it means | Fix |
| --- | --- | --- |
| `1` | No removable card found. | Insert the card and power-cycle. |
| `2` with `Mounted: false` | The logs folder couldn't be created. | Reformat the card as FAT32. |
| `Exception: …` | Writing the log failed. | The card may be full or have been removed. |

### Odometry (at INIT)

| Symptom | What it means | Fix |
| --- | --- | --- |
| `Odo Status` and `Loop Number` keep counting up and init never finishes | The Pinpoint never reported ready. | Check the wiring and that it's named `odo` in the configuration, then restart the opMode. This is a known intermittent issue. |

## Behaviour

**The robot creeps slowly toward the target instead of driving fast.**
It is in the PID fallback because the tables have nothing useful at its current state. Usually the
`startingPose()` is wrong, or it's outside the table area (the tables are inset from the walls by
about the robot's footprint). Double-check the starting pose and units: centimetres and radians.

**The robot drives confidently in the wrong direction, or spins.**
Something disagrees about which way is which. In this order, check:
1. the motor directions in `RobotParams`;
2. the odometry encoder directions;
3. the odometry offsets;
4. the starting heading.

**The robot reaches the target but the opMode keeps running.**
That's expected for now. opModes run until you press stop.

**The robot overshoots or oscillates during the final approach.**
The drivetrain model probably doesn't match the robot any more, for example after a new floor or
different wheels. [Re-calibrate](calibration.md) and re-solve.

**The robot twitches between directions while driving.**
Try raising `deadZone` or `window` in [Dashboard](tuning.md).

**`Drive` crashes at INIT with a `NullPointerException`.**
The target name doesn't exist on the card. Names must match `MANIFEST.JSON` exactly.
