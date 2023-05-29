package org.square16.ictdroid.logcat.parser;

import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.logcat.annotations.LogcatParser;
import org.square16.ictdroid.logcat.interfaces.ILogcatParser;
import org.square16.ictdroid.logcat.utils.LogInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Zsx
 */
@Slf4j
@LogcatParser(verbosity = ILogcatParser.LOGCAT_V_YEAR)
public class YearLogParser implements ILogcatParser {
    private static final String LOGCAT_REGEXP_YEAR = "(?<time>[\\d-]+\\s[\\d:.]+)\\s+" +
            "(?<pid>\\d+)\\s+(?<tid>\\d+)\\s+(?<level>[A-Z])\\s+(?<tag>\\S*)\\s*:\\s";

    @Override
    public LogInfo parse(String logLine) {
        LogInfo res = new LogInfo();
        Pattern pattern = Pattern.compile(LOGCAT_REGEXP_YEAR);
        Matcher matcher = pattern.matcher(logLine);
        if (!matcher.find() || matcher.groupCount() != 5) {
            log.warn("Unknown log line format: {}", logLine);
            res.original = logLine;
            return res;
        }
        res.time = matcher.group("time");
        res.pid = matcher.group("pid");
        res.tid = matcher.group("tid");
        res.level = matcher.group("level");
        res.tag = matcher.group("tag");
        res.msg = matcher.replaceFirst("");
        res.original = logLine;
        return res;
    }
}
