package org.square16.ictdroid.logcat.interfaces;

import org.square16.ictdroid.logcat.utils.LogInfo;

/**
 * @author Zsx
 */
public interface ILogcatParser {
    public static final String LOGCAT_V_YEAR = "year";

    /**
     * Parse log line into LogInfo
     * @param logLine log line
     * @return LogInfo
     */
    LogInfo parse(String logLine);
}
