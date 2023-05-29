package org.square16.ictdroid.logcat;

import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.utils.ADBInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Queue;

/**
 * @author Zsx
 */
@Slf4j
public class LogcatProxier {
    protected Queue<String> mLogQueue;
    protected String mVerbosity;

    public LogcatProxier(Queue<String> logQueue, String verbosity) {
        this.mLogQueue = logQueue;
        this.mVerbosity = verbosity;
    }

    public void readLogcat() {
        ADBInterface adb = ADBInterface.getInstance();
        Process logcatProcess = adb.getLogcatProcess(this.mVerbosity);
        BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
        String s;
        try {
            while ((s = reader.readLine()) != null) {
                this.mLogQueue.offer(s);
            }
        } catch (IOException e) {
            log.error("IOException when reading logcat", e);
        }
        // Restart LogcatProxyThread
        this.start();
    }

    public void start() {
        Thread proxyThread = new Thread("LogcatProxyThread") {
            @Override
            public void run() {
                readLogcat();
            }
        };
        proxyThread.setDaemon(true);
        proxyThread.start();
        log.info("Logcat proxy thread started");
    }
}
