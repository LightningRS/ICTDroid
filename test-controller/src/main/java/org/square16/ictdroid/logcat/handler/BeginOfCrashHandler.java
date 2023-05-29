package org.square16.ictdroid.logcat.handler;

import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.logcat.LogcatMonitor;
import org.square16.ictdroid.logcat.annotations.LogcatHandler;
import org.square16.ictdroid.logcat.interfaces.ILogcatHandler;
import org.square16.ictdroid.logcat.utils.LogInfo;

/**
 * @author Zsx
 */
@Slf4j
@LogcatHandler(name = "BeginOfCrashHandler", regex = "^[-\\s]+beginning of crash", priority = 20)
public class BeginOfCrashHandler implements ILogcatHandler {
    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        monitor.getTestController().getCompStateMonitor().onBeginOfCrash();
    }
}
