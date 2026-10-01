package org.firstinspires.ftc.teamcode.peregrineqa;

import com.acmerobotics.dashboard.config.Config;

/**
 * <h3>Settings for the pathing tests in PeregrineQA, editable from FTC Dashboard.</h3>
 *
 * <p>Leave {@link #driveTarget} empty to skip the "Drive reaches a target" test.</p>
 */
@Config
public final class PeregrineQAConfig {

    // Target name from MANIFEST.JSON used by the solve() and Drive tests. Empty skips the Drive test
    // and makes the solve() test use the first target.
    public static String driveTarget = "";

    // Where you will place the robot for those tests, in the field frame. Must be inside the table area.
    public static double startX = 0;        // cm
    public static double startY = 0;        // cm
    public static double startHeading = 0;  // rad

    // How long Drive gets to arrive before the test fails, in milliseconds.
    public static long driveTimeoutMs = 20000;
}
