package org.square16.ictdroid.logcat;

import org.square16.ictdroid.logcat.interfaces.ILogcatParser;
import org.square16.ictdroid.logcat.utils.LogInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogParserTest {
    @Test
    void testYearParser1() {
        String log = "2022-05-07 13:45:39.597  1310  1812 E ActivityManager: java.lang.Throwable";
        ILogcatParser parser = LogcatParserFactory.getParser(ILogcatParser.LOGCAT_V_YEAR);
        assertNotNull(parser);
        LogInfo result = parser.parse(log);
        assertEquals(result.time, "2022-05-07 13:45:39.597");
        assertEquals(result.pid, "1310");
        assertEquals(result.tid, "1812");
        assertEquals(result.level, "E");
        assertEquals(result.tag, "ActivityManager");
        assertEquals(result.msg, "java.lang.Throwable");
        assertEquals(result.original, log);
    }
}