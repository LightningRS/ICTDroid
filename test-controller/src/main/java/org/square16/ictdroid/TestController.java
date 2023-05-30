package org.square16.ictdroid;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.logcat.LogcatMonitor;
import org.square16.ictdroid.logcat.handler.StackTraceHandler;
import org.square16.ictdroid.logcat.utils.TraceBlock;
import org.square16.ictdroid.rpc.CompStateMonitor;
import org.square16.ictdroid.rpc.RPCController;
import org.square16.ictdroid.testcase.ACTSTestcaseBuilder;
import org.square16.ictdroid.testcase.BaseTestcaseBuilder;
import org.square16.ictdroid.testcase.ScopeConfig;
import org.square16.ictdroid.testcase.provider.ValueProviderICCBot;
import org.square16.ictdroid.testcase.provider.ValueProviderPreset;
import org.square16.ictdroid.testcase.provider.ValueProviderRandomWithStruct;
import org.square16.ictdroid.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
@Setter
@Slf4j
public class TestController {
    public static final int STATE_READY = 0;
    public static final int STATE_LOADING_TESTCASE = 1;
    public static final int STATE_LOADED_TESTCASE = 2;
    public static final int STATE_RUNNING_TESTCASE = 3;
    public static final int STATE_ERROR = 4;

    private static final Config GlobalConfig = Config.getInstance();
    private static final ADBInterface adb = ADBInterface.getInstance();
    private final LogcatMonitor logcatMonitor;
    private final CompStateMonitor compStateMonitor;
    private final ScopeConfig scopeConfig;
    private RPCController rpcController;
    private List<Path> apksPath;
    private AppModel currAppModel;
    private CompModel currCompModel;
    private Integer currCompState = STATE_READY;
    private Integer currCaseCount = 0;

    public TestController() {
        logcatMonitor = new LogcatMonitor(this);
        compStateMonitor = new CompStateMonitor();
        rpcController = null;
        scopeConfig = new ScopeConfig();
        boolean hasPath = GlobalConfig.getScopeConfigPath() != null;
        scopeConfig.loadScopeConfig(
                hasPath ? GlobalConfig.getScopeConfigPath().toString() : Constants.DEFAULT_SCOPE_CONFIG, hasPath
        );
    }

    public void start() {
        // Load APKs
        this.loadApks();

        if (GlobalConfig.getTestGenMode() != TestGenMode.ONLY) {
            // Initialize ADB
            adb.setADBExecPath(GlobalConfig.getAdbPath().toString());
            if (!adb.connect(GlobalConfig.getDeviceSerial())) {
                return;
            }

            // Initialize logcat
            this.logcatMonitor.start();

            if (GlobalConfig.getBridgePort() == 0) {
                // Restart test bridge
                adb.forceStopApp(Constants.CLIENT_PKG_NAME);
                // We have to stop all apps for at least once.
                for (Path apkPath : this.apksPath) {
                    this.loadApk(apkPath);
                    adb.forceStopApp(currAppModel.getPackageName());
                }
                adb.startActivity(Constants.CLIENT_PKG_NAME, Constants.CLIENT_ACT_NAME);
            } else {
                this.initRPC(GlobalConfig.getBridgePort());
            }

            // Wait until RPCController ready
            while (rpcController == null || !rpcController.isReady()) {
                Thread.yield();
            }
        }
        this.process();
    }

    public void initRPC(Integer remotePort) {
        Integer localPort = adb.forward(remotePort);
        if (this.rpcController == null) {
            this.rpcController = new RPCController(this, localPort);
        } else {
            this.rpcController.setForwardPort(localPort);
        }
        this.rpcController.init();
    }

    public void process() {
        int currApkIndex = GlobalConfig.getStartApkIndex();
        int currCompIndex = GlobalConfig.getStartCompIndex();
        int currCaseIndex = GlobalConfig.getStartCaseIndex();
        while (currApkIndex < apksPath.size()) {
            // int apkUniqueCrashesCnt = 0;
            log.info("Processing APK [{}] ({}/{})", apksPath.get(currApkIndex), currApkIndex + 1, apksPath.size());
            Path apkPath = apksPath.get(currApkIndex);
            boolean apkSkipFlag = false;

            if (!loadApk(apkPath)) {
                log.error("Failed to load APK #{}: {}, skipped", currApkIndex + 1, apkPath);
                apkSkipFlag = true;
            }

            // Check whether the APK is installed
            if (!apkSkipFlag && GlobalConfig.getTestGenMode() != TestGenMode.ONLY && !adb.isPackageInstalled(currAppModel.getPackageName())) {
                log.error("APK [{}] ({}) is not installed! Installing...", currAppModel.getPackageName(), apkPath);
                if (!adb.installSync(apkPath.toString())) {
                    log.error("Failed to install apk [{}] ({})! Skip test", currAppModel.getPackageName(), apkPath);
                    apkSkipFlag = true;
                }
            }

            if (apkSkipFlag) {
                // Reset recovery parameters
                GlobalConfig.setStartStrategy(null);
                currCaseIndex = 0;
                currCompIndex = 0;
                currApkIndex++;

                // Continue for next apk
                continue;
            }

            while (currCompIndex < currAppModel.getCompCount()) {
                // Stack trace body -> Failed Message
                // Map<String, String> uniqueStackTraces = new HashMap<>();

                currCompModel = currAppModel.getCompModelByIndex(currCompIndex);
                boolean skipFlag = false;

                // Skip inner class
                if (currCompModel.getClassName().contains("$")) {
                    log.warn("Component [{}] is an inner class, skip test", currCompModel.getClassName());
                    skipFlag = true;
                }

                // Check whether the component is enabled
                if (!skipFlag && Boolean.FALSE.equals(currCompModel.getEnabled())) {
                    log.warn("Component [{} is not enabled, skip test", currCompModel.getClassName());
                    skipFlag = true;
                }

                // Check whether the component is exported
                if (!skipFlag && Boolean.FALSE.equals(currCompModel.getExported()) && GlobalConfig.getOnlyExported()) {
                    log.warn("Component [{}] is not exported, skip test", currCompModel.getClassName());
                    skipFlag = true;
                }

                // -------------- Only support activity now --------------
                if (!skipFlag && !currCompModel.getType().equals(CompModel.TYPE_ACTIVITY)) {
                    log.warn("Component [{}] has unsupported type [{}], skip test",
                            currCompModel.getClassName(), currCompModel.getType());
                    skipFlag = true;
                }

                // Skip current component
                if (skipFlag) {
                    GlobalConfig.setStartStrategy(null);
                    currCaseIndex = 0;
                    currCompIndex++;
                    continue;
                }

                log.info("Processing component [{}] ({}/{})",
                        currCompModel.getClassName(), currCompIndex + 1, currAppModel.getCompCount());

                // Detect current strategy settings
                Collection<String> readyStrategies = new HashSet<>();
                Collection<String> pendingStrategies = new HashSet<>();
                String valueStrategy = GlobalConfig.getStrategy();
                List<String> strategyGroups = Arrays.stream(valueStrategy.split(";"))
                        .distinct().filter(Predicate.not(String::isBlank)).map(String::trim).toList();
                for (String strategyGroup : strategyGroups) {
                    List<String> providerNames = Arrays.stream(strategyGroup.split("\\+"))
                            .distinct().filter(Predicate.not(String::isBlank)).sorted().map(String::trim).toList();
                    String strategyName = String.join("+", providerNames);

                    Path compTestcasePath = Paths.get(GlobalConfig.getTestcasePath().toString(),
                            currAppModel.getPackageName() + "/" + currCompModel.getClassName() + "_" +
                                    strategyName + ".csv");
                    if (Files.exists(compTestcasePath)) {
                        log.info("Testcases of strategy [{}] are already exist", strategyName);
                        readyStrategies.add(strategyName);
                    } else {
                        pendingStrategies.add(strategyName);
                    }
                }

                if (!GlobalConfig.getTestGenMode().equals(TestGenMode.NONE)) {
                    // Only generate pending strategies
                    /* Map<String, BaseValueProvider> valueProviders = new HashMap<>() {
                        {
                            put("preset", new ValueProviderPreset(currCompModel));
                            put("iccBot", new ValueProviderICCBot(
                                    currCompModel, currCompModel.getFullValueSet(), scopeConfig
                            ));
                            put("random", new ValueProviderRandom(
                                    currCompModel, GlobalConfig.getRandValNum(),
                                    GlobalConfig.getStrMinLength(), GlobalConfig.getStrMaxLength()
                            ));
                            put("randomWithStruct", new ValueProviderRandomWithStruct(
                                    currCompModel, currCompModel.getFullValueSet(),
                                    scopeConfig, GlobalConfig.getRandValNum(),
                                    GlobalConfig.getStrMinLength(), GlobalConfig.getStrMaxLength()
                            ));
                        }
                    }; */
                    for (String strategyName : pendingStrategies) {
                        Path outputPath = Paths.get(
                                GlobalConfig.getTestcasePath().toString(), currAppModel.getPackageName(),
                                currCompModel.getClassName() + "_" + strategyName + ".csv");
                        Path outputDir = outputPath.getParent().toAbsolutePath();
                        if (!Files.exists(outputDir)) {
                            try {
                                Files.createDirectories(outputDir);
                            } catch (IOException e) {
                                log.error("Failed to create directory [" + outputDir + "]");
                                System.exit(1);
                            }
                        }
                        log.info("Generating by " + strategyName);
                        BaseTestcaseBuilder builder = new ACTSTestcaseBuilder(currCompModel, scopeConfig);
                        /* List<String> providerNames = Arrays.stream(strategyName.split("\\+"))
                                .map(String::trim).toList();
                        for (String providerName : providerNames) {
                            builder.addValueProvider(valueProviders.get(providerName));
                        } */
                        builder.addValueProvider(new ValueProviderPreset(currCompModel));
                        builder.addValueProvider(new ValueProviderICCBot(
                                currCompModel, currCompModel.getFullValueSet(), scopeConfig
                        ));
                        builder.addValueProvider(new ValueProviderRandomWithStruct(
                                currCompModel, currCompModel.getFullValueSet(),
                                scopeConfig, GlobalConfig.getRandValNum(),
                                GlobalConfig.getStrMinLength(), GlobalConfig.getStrMaxLength()
                        ));
                        builder.collect();
                        builder.build(outputPath.toString());
                        if (Files.exists(outputPath)) {
                            readyStrategies.add(strategyName);
                            log.info("Finished generating by " + strategyName);
                        } else {
                            log.warn("No testcase generated by " + strategyName);
                        }
                    }
                    pendingStrategies.removeAll(readyStrategies);

                    if (GlobalConfig.getTestGenMode().equals(TestGenMode.ONLY)) {
                        currCompIndex++;
                        continue;
                    }
                }
                if (pendingStrategies.size() > 0) {
                    // Report error if there are still pending strategies
                    log.warn("No testcases under strategies [{}] for component [{}]",
                            String.join(", ", pendingStrategies), currCompModel.getClassName());
                }
                log.info("{} strategies [{}] are ready for testing component [{}]", readyStrategies.size(),
                        String.join(", ", readyStrategies), currCompModel.getClassName());

                // Resort ready strategies
                SortedArrayList<String> sortedReadyStrategies = new SortedArrayList<>();
                sortedReadyStrategies.addAll(readyStrategies);
                readyStrategies = sortedReadyStrategies;

                // Push all testcases
                for (String strategy : readyStrategies) {
                    Path localPath = Paths.get(
                            GlobalConfig.getTestcasePath().toString(), currAppModel.getPackageName(),
                            currCompModel.getClassName() + "_" + strategy + ".csv");
                    Path remotePath = Paths.get(
                            Constants.CLIENT_CASE_ROOT, currAppModel.getPackageName(),
                            currCompModel.getClassName() + "_" + strategy + ".csv");
                    log.debug("Pushing testcase [{}]", localPath.getFileName());
                    if (!adb.pushSync(localPath.toString(), remotePath.toString().replaceAll("\\\\", "/"))) {
                        log.error("Failed to push testcase [{}] to [{}]!", localPath, remotePath);
                        if (!GlobalConfig.getContinueIfError()) {
                            return;
                        }
                    }
                }
                log.info("Finished pushing all testcases to client");

                compStateMonitor.setComponent(currAppModel.getPackageName(), currCompModel.getClassName(),
                        currCompModel.getType());
                for (String strategy : readyStrategies) {
                    if (GlobalConfig.getStartStrategy() != null && !GlobalConfig.getStartStrategy().equals(strategy)) {
                        continue;
                    }
                    GlobalConfig.setStartStrategy(null);
                    log.info("Using strategy [{}]", strategy);
                    // Waiting for testcase load finished
                    setCurrCompState(STATE_LOADING_TESTCASE);
                    rpcController.loadTestcase(currAppModel.getPackageName(), currCompModel.getClassName(),
                            currCompModel.getType(), strategy);
                    while (currCompState == STATE_LOADING_TESTCASE) {
                        Thread.yield();
                    }
                    if (currCompState != STATE_LOADED_TESTCASE) {
                        log.error("Unexpected state after loading case! state={}, comp={}, strategy={}",
                                currCompState, currCompModel.getClassName(), strategy);
                        if (GlobalConfig.getContinueIfError()) {
                            currCompIndex++;
                            continue;
                        } else {
                            return;
                        }
                    }

                    // Run testcases
                    int caseRetryCnt = 0;
                    while (currCaseIndex < currCaseCount) {
                        String recoveryInfo = String.format(
                                "compName=%s, apkIndex=%s, compIndex=%s, caseIndex=%s, strategy=%s",
                                currCompModel.getClassName(), currApkIndex, currCompIndex, currCaseIndex, strategy
                        );

                        adb.forceStopApp(currAppModel.getPackageName());
                        log.info("Start to run testcase #{}/{}", currCaseIndex + 1, currCaseCount);
                        // Waiting for testcase run finished
                        setCurrCompState(STATE_RUNNING_TESTCASE);
                        rpcController.runTestcase(currCaseIndex);
                        compStateMonitor.setCompState(CompStateMonitor.STATE_UNK);

                        compStateMonitor.setComponent(currAppModel.getPackageName(),
                                currCompModel.getClassName(), currCompModel.getType());
                        compStateMonitor.start();

                        while (compStateMonitor.getCompState() < CompStateMonitor.STATE_RESULT) {
                            Thread.yield();
                        }

                        // Waiting for consuming all trace blocks
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        StackTraceHandler stHandler = logcatMonitor.getHandlerByClass(StackTraceHandler.class);
                        TraceBlock mergedTraceBlock = new TraceBlock();
                        while (stHandler.hasTraceBlock()) {
                            TraceBlock tb = stHandler.getTraceBlock();
                            if (tb.body.contains(currAppModel.getPackageName()) ||
                                    tb.body.contains(Constants.CLIENT_PKG_NAME)) {
                                if (mergedTraceBlock.headInfo == null) {
                                    mergedTraceBlock.headInfo = tb.headInfo;
                                }
                                if (mergedTraceBlock.body != null) {
                                    mergedTraceBlock.body += "\n" + tb.body;
                                } else {
                                    mergedTraceBlock.body = tb.body;
                                }
                            }
                        }

                        if (compStateMonitor.getCompState() <= CompStateMonitor.STATE_CLIENT_ERROR) {
                            log.error("Case running ERROR! Try to rollback! state={}, mFocusedActivity={}, {}",
                                    compStateMonitor.getCompStateName(), compStateMonitor.getFocusedActivity(),
                                    recoveryInfo);
                            caseRetryCnt++;
                            if (caseRetryCnt > Constants.CASE_MAX_RETRY) {
                                log.error("Max retry time of running case exceed! Skip the current case!");
                                currCaseIndex++;
                            }
                            continue;
                        }
                        caseRetryCnt = 0;

                        // Retrieve stack trace block
                        if (mergedTraceBlock.headInfo == null) {
                            if (compStateMonitor.getCompState() < CompStateMonitor.STATE_JUMPED) {
                                log.error("No stacktrace is caught!");
                            }
                        } else {
                            log.warn("Stacktrace caught: {}\n{}", mergedTraceBlock.headInfo, mergedTraceBlock.body);
                            if (mergedTraceBlock.body.contains("android.content.ActivityNotFoundException")) {
                                // If ActivityNotFoundException detected, it means the component is failed to find.
                                // Usually it is due to the android:enabled="false" defined in AndroidManifest.xml.
                                // Should skip the component directly.
                                log.error("ActivityNotFoundException occurred, skip the current component!");
                                log.info("Finished running testcase #{}", currCaseIndex + 1);
                                break;
                            }
                        }

                        if (compStateMonitor.getCompState() < CompStateMonitor.STATE_JUMPED) {
                            log.error("Case FAILED! state={}, mFocusedActivity={}, {}",
                                    compStateMonitor.getCompStateName(), compStateMonitor.getFocusedActivity(),
                                    recoveryInfo);

                            if (compStateMonitor.getCompState() >= CompStateMonitor.STATE_APP_CRASHED) {
                                // if (mergedTraceBlock.headInfo != null &&
                                //         !uniqueStackTraces.containsKey(mergedTraceBlock.body)) {
                                //     String resultMsg = String.format(
                                //             "state=%s, %s", compStateMonitor.getCompStateName(), recoveryInfo
                                //     );
                                //     uniqueStackTraces.put(mergedTraceBlock.body, resultMsg);
                                //     log.info("Unique crash #{} detected! " + resultMsg, uniqueStackTraces.size());
                                // }
                                if (!GlobalConfig.getContinueIfError()) {
                                    return;
                                }
                            }

                            // Close all system dialogs to avoid window stack overflow
                            adb.closeSystemDialogs();
                        } else if (compStateMonitor.getCompState().equals(CompStateMonitor.STATE_JUMPED)) {
                            log.info("Case JUMPED! mFocusedActivity={}, {}",
                                    compStateMonitor.getFocusedActivity(), recoveryInfo);
                        } else {
                            log.info("Case PASSED! state={}, mFocusedActivity={}, {}",
                                    compStateMonitor.getCompStateName(), compStateMonitor.getFocusedActivity(),
                                    recoveryInfo);
                        }
                        // Testcase finished
                        log.info("Finished running testcase #{}", currCaseIndex + 1);
                        currCaseIndex++;
                    }
                    currCaseIndex = 0;
                }
                adb.forceStopApp(currAppModel.getPackageName());
                log.info("Finished testing component [{}]", currCompModel.getClassName());
                // log.info("Finished testing component [{}], with {} unique crashes",
                //     currCompModel.getClassName(), uniqueStackTraces.size());
                //     apkUniqueCrashesCnt += uniqueStackTraces.size();
                currCompIndex++;
            }
            if (!GlobalConfig.getTestGenMode().equals(TestGenMode.ONLY)) {
                adb.forceStopApp(currAppModel.getPackageName());
                // log.info("Finished testing APK [{}], with {} unique crashes", apksPath.get(currApkIndex), apkUniqueCrashesCnt);
                log.info("Finished testing APK [{}]", apksPath.get(currApkIndex));
            } else {
                log.info("Finished generating testcases for APK [{}]", apksPath.get(currApkIndex));
            }
            currCompIndex = 0;
            currApkIndex++;
        }
    }

    private void loadApks() {
        apksPath = new ArrayList<>();
        if (!GlobalConfig.isApkPathDirectory()) {
            apksPath.add(GlobalConfig.getApkPath());
        } else {
            try (Stream<Path> entries = Files.walk(GlobalConfig.getApkPath())) {
                entries.forEach(path -> {
                    if (path.toString().endsWith(".apk")) {
                        apksPath.add(path);
                    }
                });
            } catch (IOException e) {
                log.error("IOException when walking apk directory: {}", GlobalConfig.getApkPath());
            }
        }
        if (GlobalConfig.getRunApksInDescOrder()) {
            apksPath.sort((p1, p2) -> -p1.compareTo(p2));
        }
        log.info("Found totally {} apks to run", apksPath.size());
    }

    private boolean loadApk(Path apkPath) {
        try {
            currAppModel = new AppModel(apkPath);
        } catch (Exception e) {
            log.error("{} when loading apk: {}", e.getClass().getSimpleName(), apkPath, e);
            return false;
        }
        return true;
    }

    public void setCurrCompState(int state) {
        if (state < STATE_READY || state > STATE_ERROR) {
            throw new IllegalArgumentException("Invalid component state: " + state);
        }
        this.currCompState = state;
    }

    public void setCurrCaseCount(int caseCount) {
        currCaseCount = caseCount;
    }
}
