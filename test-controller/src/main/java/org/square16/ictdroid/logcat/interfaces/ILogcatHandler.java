package org.square16.ictdroid.logcat.interfaces;

import org.square16.ictdroid.logcat.LogcatMonitor;
import org.square16.ictdroid.logcat.annotations.LogcatHandler;
import org.square16.ictdroid.logcat.utils.LogInfo;

public interface ILogcatHandler extends Comparable<ILogcatHandler> {
    /**
     * Handle log info
     * @param monitor logcat monitor
     * @param logInfo log info
     */
    void handle(LogcatMonitor monitor, LogInfo logInfo);

    /**
     * Compare with another ILogcatHandler
     * @param o the ILogcatHandler object to be compared.
     * @return compare result
     */
    @Override
    default int compareTo(ILogcatHandler o) {
        LogcatHandler thisInfo = this.getClass().getAnnotation(LogcatHandler.class);
        LogcatHandler otherInfo = o.getClass().getAnnotation(LogcatHandler.class);
        if (thisInfo.priority() > otherInfo.priority()) {
            return 1;
        } else if (thisInfo.priority() < otherInfo.priority()) {
            return -1;
        } else {
            return thisInfo.name().compareTo(otherInfo.name());
        }
    }
}
