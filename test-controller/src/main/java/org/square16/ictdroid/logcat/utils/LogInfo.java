package org.square16.ictdroid.logcat.utils;

/**
 * @author Zsx
 */
public class LogInfo {
    public String time;
    public String pid;
    public String tid;
    public String level;
    public String tag;
    public String msg;
    public String original;

    @Override
    public String toString() {
        return String.format("time=%s, pid=%s, tid=%s, level=%s, tag=%s, msg=%s",
                time, pid, tid, level, tag, msg);
    }
}
