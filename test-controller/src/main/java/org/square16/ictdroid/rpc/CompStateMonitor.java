package org.square16.ictdroid.rpc;

import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.utils.ADBInterface;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CompStateMonitor {
    public static int STATE_UNK = 0;
    public static int STATE_DISPLAYED = 1;

    public static int STATE_RESULT = 2;

    public static int STATE_CLIENT_ERROR = 3;
    public static int STATE_INTENT_ERR = 4;
    public static int STATE_SYS_ERR = 5;
    public static int STATE_APP_CRASHED = 6;
    public static int STATE_TIMEOUT = 7;
    public static int STATE_DISPLAYED_TIMEOUT = 8;
    public static int STATE_JUMPED = 9;
    public static int STATE_SUCCESS = 10;

    private final ADBInterface adb = ADBInterface.getInstance();
    private final Config GlobalConfig = Config.getInstance();
    private String pkgName;
    private String compName;
    private String compType;
    private String focusedActivity;
    private Integer compState = STATE_UNK;
    private Long startedAt;
    private Long displayedAt;
    private int jumpCnt;
    private int launcherCnt;
    private boolean isStarted = false;

    public CompStateMonitor() {
    }

    public Integer getCompState() {
        return compState;
    }

    public void setCompState(Integer state) {
        if (state < STATE_UNK || state > STATE_SUCCESS) {
            throw new IllegalArgumentException("Invalid component state: " + state);
        }
        this.compState = state;
    }

    public String getCompStateName() {
        if (compState.equals(STATE_UNK)) {
            return "UNK";
        } else if (compState.equals(STATE_DISPLAYED)) {
            return "DISPLAYED";
        } else if (compState.equals(STATE_CLIENT_ERROR)) {
            return "CLIENT_ERROR";
        } else if (compState.equals(STATE_INTENT_ERR)) {
            return "INTENT_ERROR";
        } else if (compState.equals(STATE_SYS_ERR)) {
            return "SYS_ERROR";
        } else if (compState.equals(STATE_APP_CRASHED)) {
            return "APP_CRASHED";
        } else if (compState.equals(STATE_TIMEOUT)) {
            return "TIMEOUT";
        } else if (compState.equals(STATE_DISPLAYED_TIMEOUT)) {
            return "DISPLAYED_TIMEOUT";
        } else if (compState.equals(STATE_JUMPED)) {
            return "JUMPED";
        } else if (compState.equals(STATE_SUCCESS)) {
            return "SUCCESS";
        } else {
            return "INVALID";
        }
    }

    public String getFocusedActivity() {
        return focusedActivity;
    }

    public void start() {
        this.startedAt = System.currentTimeMillis();
        this.displayedAt = null;
        this.jumpCnt = 0;
        this.launcherCnt = 0;
        this.isStarted = false;
        this.compState = STATE_UNK;
        log.info("Started component state monitor for compoent [" + compName + "]");
        new Thread("CompStateMonitor") {
            @Override
            public void run() {
                while (compState < STATE_RESULT) {
                    checkOnce();
                }
            }
        }.start();
    }

    private void checkOnce() {
        long st = System.currentTimeMillis();
        // logcat is checking by ActivityDisplayedHandler
        // Check current focused activity
        checkByDumpSys();
        // Check timeout
        checkTimeout();
        while (System.currentTimeMillis() - st < Constants.COMP_CHECK_INTERVAL) {
            Thread.yield();
        }
    }

    private void checkTimeout() {
        Long currTime = System.currentTimeMillis();
        if (currTime - startedAt > Constants.TIMEOUT_START_MS && compState < STATE_DISPLAYED) {
            if (!isStarted) {
                log.warn("Component [{}] start timeout", compName);
                compState = STATE_TIMEOUT;
            } else {
                log.info("Component [{}] exited normally", compName);
                compState = STATE_SUCCESS;
            }
        }
    }

    private void checkByDumpSys() {
        String dumpSysType = switch (this.compType) {
            case CompModel.TYPE_ACTIVITY -> "window windows";
            case CompModel.TYPE_SERVICE -> "service";
            default -> null;
        };
        if (dumpSysType == null) {
            log.debug("Unsupported component type: " + this.compType);
            return;
        }
        String res = adb.shellSync("dumpsys " + dumpSysType);
        if (res == null) {
            log.warn("Call dumpsys failed");
            return;
        }
        String focusName = null;
        Pattern mCurrFocusPattern = Pattern.compile(
                "(?m)^\\s*mCurrentFocus=(?<type>[^{]+)\\{(?<id>\\S+)\\s+" +
                        "(?<user>\\S+)\\s+(?<compName>[^}]+)}$"
        );
        Matcher currFocusMatcher = mCurrFocusPattern.matcher(res);
        Pattern focusedAppPattern = Pattern.compile(
                "(?m)^\\s*mFocusedApp=(?<tokenType>[^{]+)\\{(?<outTokenId>\\S+)\\s+" +
                        "token=Token\\{(?<tokenId>\\S+)\\s+(?<recordType>[^{]+)\\{" +
                        "(?<recordId>\\S+)\\s+(?<user>\\S+)\\s+(?<compName>\\S+)\\s+(?<tName>[^}]+)}}}$"
        );
        Matcher focusedAppMatcher = focusedAppPattern.matcher(res);
        Pattern mLastClosingPattern = Pattern.compile(
                "(?m)^\\s*mLastClosingApp=(?<tokenType>[^{]+)\\{(?<outTokenId>\\S+)\\s+" +
                        "token=Token\\{(?<tokenId>\\S+)\\s+(?<recordType>[^{]+)\\{" +
                        "(?<recordId>\\S+)\\s+(?<user>\\S+)\\s+(?<compName>\\S+)\\s+(?<tName>[^}]+)}}}$"
        );
        Matcher lastClosingMatcher = mLastClosingPattern.matcher(res);

        if (currFocusMatcher.find()) {
            String currFocusCompName = currFocusMatcher.group("compName");
            if (currFocusCompName.startsWith("Application Error")) {
                if (!currFocusCompName.contains(pkgName)) {
                    log.warn("Application Error that not belongs to target app detected! name={}", currFocusCompName);
                }
                compState = STATE_APP_CRASHED;
                focusedActivity = currFocusCompName;
                return;
            } else if (currFocusCompName.contains("/")) {
                String[] focusNames = currFocusCompName.split("/");
                focusName = focusNames[0] + "/" + focusNames[1].replace(focusNames[0], "");
            } else {
                log.warn("Unrecognized component name format of mCurrentFocus: {}", currFocusCompName);
                if ("DeprecatedTargetSdkVersionDialog".equals(currFocusCompName)) {
                    adb.shellSync("input keyevent 66");
                    adb.shellSync("input keyevent 66");
                }
            }
        }
        if (focusName == null && focusedAppMatcher.find()) {
            focusName = focusedAppMatcher.group("compName");
        }
        if (focusName == null) {
            log.error("mCurrentFocus/mFocusedApp not detected in dumpsys result");
            return;
        }
        focusedActivity = focusName;
        if (compName.equals(focusedActivity)) {
            long nowTime = System.currentTimeMillis();
            if (displayedAt != null) {
                if (nowTime - displayedAt >= Constants.TIME_DISPLAY_REQUIRE_MS) {
                    compState = STATE_SUCCESS;
                }
            } else {
                displayedAt = nowTime;
            }
        } else if (focusedActivity.contains(GlobalConfig.getAndroidLauncherPkgName())) {
            launcherCnt++;
            if (launcherCnt > 3 && !isStarted) {
                // JUMPED to launcher, identify as crash
                compState = STATE_APP_CRASHED;
            }
        } else if (!focusedActivity.equals(Constants.CLIENT_COMP_NAME)) {
            // mFocusedActivity is neither component nor test client.
            // It may indicate that the component is redirected to another one.
            jumpCnt++;
            if (jumpCnt > 3) {
                compState = STATE_JUMPED;
            }
        }

        // Detect mLastClosingApp
        if (lastClosingMatcher.find()) {
            String lastClosingCompName = lastClosingMatcher.group("compName");
            if (lastClosingCompName.equals(compName) || lastClosingCompName.contains(pkgName)) {
                isStarted = true;
            }
        }
    }

    public void setComponent(String pkgName, String compName, String compType) {
        this.pkgName = pkgName;
        if (!compName.contains("/")) {
            if (!compName.contains(pkgName)) {
                compName = pkgName + "/" + compName;
            } else {
                compName = pkgName + compName.replace(pkgName, "/");
            }
        }
        this.compName = compName;
        this.compType = compType;
    }

    public void onActivityDisplayed(String logCompName) {
        if (!logCompName.equals(this.compName)) {
            return;
        }
        if (compState != STATE_UNK) {
            log.warn("Component state not UNK, ignored");
            return;
        }
        compState = STATE_DISPLAYED;
        displayedAt = System.currentTimeMillis();
        log.info("Activity displayed! Waiting for TIME_DISPLAY_REQUIRE");
    }

    public void onBeginOfCrash() {
        log.warn("Beginning of crash detected!!");
        compState = STATE_APP_CRASHED;
    }
}
