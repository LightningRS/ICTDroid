package org.square16.ictdroid.logcat.handler;

import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.logcat.LogcatMonitor;
import org.square16.ictdroid.logcat.annotations.LogcatHandler;
import org.square16.ictdroid.logcat.interfaces.ILogcatHandler;
import org.square16.ictdroid.logcat.utils.LogInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Zsx
 */
@Slf4j
@LogcatHandler(name = "ActivityDisplayedHandler", regex = "I ActivityManager: Displayed", priority = 1)
public class ActivityDisplayedHandler implements ILogcatHandler {
    private static final Pattern PATTERN = Pattern.compile("Displayed (?<compName>[^:]+): (?<startDelay>.*)");

    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        if (logInfo.msg == null) {
            return;
        }
        Matcher matcher = PATTERN.matcher(logInfo.msg);
        if (!matcher.find() || matcher.groupCount() != 2) {
            log.error("Activity displayed log pattern match failed: " + logInfo.msg);
            return;
        }
        String compName = matcher.group("compName");
        monitor.getTestController().getCompStateMonitor().onActivityDisplayed(compName);
    }
}
