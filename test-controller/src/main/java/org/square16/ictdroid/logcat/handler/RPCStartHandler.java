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
@LogcatHandler(name = "RPCStartHandler", keywords = {"Test RPCServer started at port"})
public class RPCStartHandler implements ILogcatHandler {
    private static final Pattern PATTERN = Pattern.compile("Test RPCServer started at port (?<port>\\d+)");

    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        if (logInfo.msg == null) {
            return;
        }
        Matcher matcher = PATTERN.matcher(logInfo.msg);
        if (!matcher.find()) {
            log.error("Failed to match rpc remote port! logInfo.msg: {}", logInfo.msg);
            return;
        }
        Integer port = Integer.parseInt(matcher.group("port"));
        log.info("Detected rpc remote port {}", port);
        monitor.getTestController().initRPC(port);
    }
}
