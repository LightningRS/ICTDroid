package org.square16.ictdroid.logcat.handler;

import org.square16.ictdroid.logcat.LogcatMonitor;
import org.square16.ictdroid.logcat.annotations.LogcatHandler;
import org.square16.ictdroid.logcat.interfaces.ILogcatHandler;
import org.square16.ictdroid.logcat.utils.LogInfo;

/**
 * @author Zsx
 */
@LogcatHandler(name = "BackTraceHandler", regex = ".*", priority = 1)
public class BackTraceHandler implements ILogcatHandler {
    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        StackTraceHandler stHandler = monitor.getHandlerByClass(StackTraceHandler.class);
        stHandler.setLastLineInfo(logInfo);
    }
}
