package org.square16.ictdroid.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ADBInterface {
    private Path adbExecPath;
    private String deviceSerial;
    private boolean isConnected = false;
    private ADBInterface() {

    }

    public static ADBInterface getInstance() {
        return ADBInterfaceHolder.INSTANCE;
    }

    private Process callADB(String[] args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(this.adbExecPath.toString());
        if (this.deviceSerial != null
                && !"devices".equals(args[0])
                && !"start-server".equals(args[0])
                && !"kill-server".equals(args[0])
        ) {
            cmd.add("-s");
            cmd.add(this.deviceSerial);
        }
        cmd.addAll(List.of(args));
        log.debug("Executing adb command: {}", cmd);
        try {
            return Runtime.getRuntime().exec(cmd.toArray(new String[0]));
        } catch (IOException e) {
            log.error("IOException when calling adb", e);
        }
        return null;
    }

    private Process callADBAsync(String... args) {
        return this.callADB(args);
    }

    private ADBResult callADBSync(String... args) {
        Process p = this.callADB(args);
        if (p == null) {
            return null;
        }
        StringBuilder stdoutBuffer = new StringBuilder();
        StringBuilder stderrBuffer = new StringBuilder();
        try {
            BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            int b;
            while ((b = stdout.read()) != -1) {
                stdoutBuffer.append((char) b);
            }
            while ((b = stderr.read()) != -1) {
                stderrBuffer.append((char) b);
            }
        } catch (IOException e) {
            log.error("IOException when reading adb result", e);
        }
        ADBResult result = new ADBResult();
        result.out = stdoutBuffer.length() > 0 ? stdoutBuffer.toString().trim() : null;
        result.err = stderrBuffer.length() > 0 ? stderrBuffer.toString().trim() : null;
        // log.debug("ADB result\n=== adb result start\n{}\n=== adb result end", result);
        return result;
    }

    public Path getADBExecPath() {
        return this.adbExecPath;
    }

    public void setADBExecPath(String path) {
        Path execPath = Paths.get(path).toAbsolutePath();
        if (Files.notExists(execPath)) {
            log.error(String.format("ADB executable path [%s] not found", execPath.toString()));
            throw new RuntimeException("ADB executable not found");
        }
        this.adbExecPath = execPath;
    }

    public String getADBExecPathStr() {
        return this.adbExecPath == null ? null : this.adbExecPath.toString();
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void disconnect() {
        this.isConnected = false;
    }

    public boolean connect(String deviceSerial) {
        this.deviceSerial = deviceSerial;
        return this.connect();
    }

    public boolean connect() {
        if (this.adbExecPath == null) {
            log.error("ADB executable path is not specified");
            return false;
        }
        ADBResult res = this.callADBSync("devices");
        if (res == null) {
            log.error("adb devices returned error! res:\n{}", res);
            return false;
        }
        res = this.callADBSync("get-state");
        if (res == null || res.out == null || !"device".equals(res.out)) {
            log.error("adb get-state returned error! res:\n{}", res);
            return false;
        }
        log.info("adb connected to device{}", this.deviceSerial != null ? " [" + this.deviceSerial + "]" : "");
        this.isConnected = true;
        return true;
    }

    public String shellSync(String command) {
        String[] shellCommand = Arrays.asList("shell", command).toArray(String[]::new);
        ADBResult res = this.callADBSync(shellCommand);
        if (res == null) {
            log.error("Failed to call adb shell: no result");
            return null;
        } else if (res.err != null) {
            log.error("Failed to call adb shell: error");
            log.error(res.toString());
            return null;
        }
        return res.out;
    }

    public Integer forward(Integer remotePort) {
        return this.forward("tcp:0", "tcp:" + remotePort);
    }

    public Integer forward(Integer localPort, Integer remotePort) {
        return this.forward("tcp:" + localPort, "tcp:" + remotePort);
    }

    private Integer forward(String local, String remote) {
        String localParam = local.startsWith("tcp:") ? local : "tcp:" + local;
        String remoteParam = remote.startsWith("tcp:") ? remote : "tcp:" + remote;
        ADBResult res = this.callADBSync("forward", localParam, remoteParam);
        if (res == null || res.err != null) {
            log.error("Failed to call adb forward, res={}", res);
            return null;
        }
        if (res.out == null) {
            // The local port forwarding is already exists
            log.error("Local address [{}] is already used for forwarding", localParam);
            return null;
        }
        String[] splitRes = res.out.split(":");
        return Integer.parseInt(splitRes[splitRes.length - 1]);
    }

    public boolean removeForward(Integer localPort) {
        return this.removeForward("tcp:" + localPort);
    }

    private boolean removeForward(String local) {
        String localParam = local.startsWith("tcp:") ? local : "tcp:" + local;
        ADBResult res = this.callADBSync("forward", "--remove", localParam);
        if (res == null || res.err != null) {
            log.warn("Failed to call adb forward --remove, res={}", res);
            return false;
        }
        if (res.out == null) {
            log.info("Removed port forwarding on [{}]", localParam);
            return true;
        }
        log.warn("Failed to remove port forwarding on [{}], res={}", localParam, res);
        return false;
    }

    public Process getLogcatProcess() {
        return this.getLogcatProcess("year");
    }

    public Process getLogcatProcess(String verbosity) {
        this.callADBSync("logcat", "-c");
        return this.callADBAsync("logcat", "-v", verbosity);
    }

    public boolean pushSync(String localPath, String remotePath) {
        String[] pushCommand = Arrays.asList("push", localPath, remotePath).toArray(String[]::new);
        ADBResult res = this.callADBSync(pushCommand);
        if (res == null || res.err != null) {
            log.error("Failed to call adb push, res={}", res);
            return false;
        }
        return res.out.contains("pushed,");
    }

    /**
     * Check whether a package is installed or not.
     *
     * @param packageId Package ID
     * @return true if installed, false otherwise
     */
    public boolean isPackageInstalled(String packageId) {
        String command = "pm list packages | grep \"" + packageId + "\"";
        String res = this.shellSync(command);
        return res != null && !"".equals(res.trim());
    }

    /**
     * Install APK sync
     *
     * @param apkPath Path to apk file
     * @return true if installed, false otherwise
     */
    public boolean installSync(String apkPath) {
        String[] command = Arrays.asList("install", "-g", apkPath).toArray(String[]::new);
        ADBResult res = this.callADBSync(command);
        if (res == null) {
            return false;
        }
        if (res.out != null && res.out.contains("Success")) {
            return true;
        }
        if (res.err != null && res.err.contains("INSTALL_FAILED_ALREADY_EXISTS")) {
            return true;
        }
        log.error("Failed to install APK [{}]", apkPath);
        log.error(res.toString());
        return false;
    }

    /**
     * Uninstall package sync
     *
     * @param packageId Package ID
     * @return true if uninstalled, false otherwise
     */
    public boolean uninstallSync(String packageId) {
        String[] shellCommand = Arrays.asList("shell", "pm uninstall \"" + packageId + "\"").toArray(String[]::new);
        ADBResult res = this.callADBSync(shellCommand);
        if (res == null) {
            log.error("Failed to uninstall package [{}]: no result", packageId);
            return false;
        }
        if (res.out != null && res.out.contains("Success")) {
            return true;
        }
        if (res.err != null && res.err.contains("Unknown package")) {
            return true;
        }
        log.error("Failed to uninstall package [{}]: error", packageId);
        log.error(res.toString());
        return false;
    }

    /**
     * Force stop an application (using <b>am force-stop</b>).
     *
     * @param pkgId App Package ID
     */
    public void forceStopApp(String pkgId) {
        String command = "am force-stop \"" + pkgId + "\"";
        this.shellSync(command);
    }

    /**
     * Close all system dialogs to avoid window stack overflow.
     */
    public void closeSystemDialogs() {
        String command = "am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS";
        this.shellSync(command);
    }

    /**
     * Force stop an application (using <b>am start</b>).
     *
     * @param pkgId App Package ID
     */
    public boolean startActivity(String pkgId, String actId) {
        String command = "am start \"" + pkgId + "/" + actId + "\"";
        String res = this.shellSync(command);
        return res != null && !res.contains("Error:");
    }

    private static class ADBResult {
        private String out;
        private String err;

        @Override
        public String toString() {
            return String.format("""
                    ADBResult dump
                    ======== STDOUT ========
                    %s
                    ======== STDERR ========
                    %s
                    ========================""", out, err);
        }
    }

    private static class ADBInterfaceHolder {
        private static final ADBInterface INSTANCE = new ADBInterface();
    }
}
