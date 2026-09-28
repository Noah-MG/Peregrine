package org.firstinspires.ftc.teamcode.qa;

import com.acmerobotics.dashboard.config.Config;

/**
 * <h3>Settings for a QA run, editable from FTC Dashboard.</h3>
 *
 * <p>Set these in Dashboard's Configuration panel before pressing INIT on a QRRoutine.</p>
 */
@Config
public final class QAConfig {

    // Who is running the tests. Written at the top of the log.
    public static String tester = "Unnamed Tester";
    // The log's file name, without ".csv". A QRRoutine refuses to start while this is still "Unnamed Log".
    public static String logName = "Unnamed Log";

}
