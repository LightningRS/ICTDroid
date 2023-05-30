package org.square16.ictdroid.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.square16.ictdroid.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ArgParser {
    private static final Options OPTIONS = getOptions();

    public static boolean parse(String[] args) {
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(OPTIONS, args);
        } catch (ParseException e) {
            log.error("Invalid options: {}", e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            System.out.print("\n");
            formatter.printHelp(
                    120,
                    "java -jar ICTDroid.jar",
                    "\nICTDroid: Parameter-Aware Combinatorial Testing for Components of Android Apps\nVersion: 1.0.1",
                    OPTIONS,
                    "\nFor more information, please refers to https://lightningrs.github.io/tools/ICTDroid.html\n\n"
            );
            return false;
        }
        Config config = Config.getInstance();

        // ------------------------ Non-null parameters ------------------------

        Path apkPath = Paths.get(cmd.getOptionValue("k")).toAbsolutePath();
        if (!Files.isReadable(apkPath)) {
            log.error("APK path is not readable: {}", apkPath);
            return false;
        }
        config.setApkPath(apkPath);
        config.setApkPathDirectory(Files.isDirectory(apkPath));

        Path iccResultPath = Paths.get(cmd.getOptionValue("i")).toAbsolutePath();
        if (!Files.isDirectory(iccResultPath) || !Files.isReadable(iccResultPath)) {
            log.error("Invalid ICCBot result path: {}", iccResultPath);
            return false;
        }
        config.setIccResultPath(iccResultPath);

        Path testcasePath = Paths.get(cmd.getOptionValue("c")).toAbsolutePath();
        if (!Files.isDirectory(testcasePath) || !Files.isReadable(testcasePath)) {
            log.error("Invalid testcase directory: {}", testcasePath);
            return false;
        }
        config.setTestcasePath(testcasePath);

        // config.setStrategy(cmd.getOptionValue("st"));
        config.setStrategy("base");

        // ------------------------ Nullable parameters ------------------------

        if (cmd.hasOption("o")) {
            Path scopeConfigPath = Paths.get(cmd.getOptionValue("o")).toAbsolutePath();
            if (!Files.isRegularFile(scopeConfigPath) || !Files.isReadable(scopeConfigPath)) {
                log.error("Invalid scope config file: {}", scopeConfigPath);
                return false;
            }
            config.setScopeConfigPath(scopeConfigPath);
        }

        config.setContinueIfError(cmd.hasOption("ce"));
        config.setOnlyExported(cmd.hasOption("oe"));
        config.setRunApksInDescOrder(cmd.hasOption("rd"));
        config.setSeed(cmd.hasOption("s") ? Long.parseLong(cmd.getOptionValue("s")) : System.currentTimeMillis());
        config.setDeviceSerial(cmd.getOptionValue("d"));

        if (cmd.hasOption("mr")) {
            Path mistResultPath = Paths.get(cmd.getOptionValue("mr")).toAbsolutePath();
            if (!Files.isRegularFile(mistResultPath) || !Files.isReadable(mistResultPath)) {
                log.error("Invalid MIST result path: {}", mistResultPath);
                return false;
            }
            try {
                config.setMISTResult(JSON.parseObject(Files.readString(mistResultPath)));
            } catch (IOException e) {
                log.error("Failed to load MIST result: {}", mistResultPath);
                return false;
            }
        }

        int s = 0;
        if (cmd.hasOption("ss")) {
            String ss = cmd.getOptionValue("ss");
            try {
                s = Integer.parseInt(ss);
                if (s < 0 || s > 6) {
                    throw new NumberFormatException("Strength can only be in [0, 6]");
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid strength strategy [{}], use 0 (dynamic) instead", ss, e);
            }
        }
        config.setDefaultStrength(s);

        config.setWithManifest(cmd.hasOption("wm"));
        config.setWithRandom(cmd.hasOption("wr"));
        config.setWithPresetAndBoundary(cmd.hasOption("wp"));

        config.setStartApkIndex(cmd.hasOption("ia") ? Integer.parseInt(cmd.getOptionValue("ia")) : 0);
        config.setStartCompIndex(cmd.hasOption("ic") ? Integer.parseInt(cmd.getOptionValue("ic")) : 0);
        config.setStartCaseIndex(cmd.hasOption("it") ? Integer.parseInt(cmd.getOptionValue("it")) : 0);
        config.setStartStrategy(cmd.getOptionValue("is"));

        config.setRandValNum(cmd.hasOption("r") ?
                Integer.parseInt(cmd.getOptionValue("r")) :
                Constants.DEFAULT_RAND_VAL_NUM);
        config.setStrMinLength(cmd.hasOption("smin") ?
                Integer.parseInt(cmd.getOptionValue("smin")) :
                Constants.DEFAULT_RAND_STR_MIN_LENGTH);
        config.setStrMaxLength(cmd.hasOption("smax") ?
                Integer.parseInt(cmd.getOptionValue("smax")) :
                Constants.DEFAULT_RAND_STR_MAX_LENGTH);

        if (cmd.hasOption("ag")) {
            config.setTestGenMode(TestGenMode.AUTO);
        } else if (cmd.hasOption("og")) {
            config.setTestGenMode(TestGenMode.ONLY);
        } else {
            config.setTestGenMode(TestGenMode.NONE);
        }

        if (!cmd.hasOption("og")) {
            // To run test, check parameters
            if (!cmd.hasOption("a")) {
                log.error("ADB executable path cannot be empty");
                return false;
            }
            Path adbPath = Paths.get(cmd.getOptionValue("a")).toAbsolutePath();
            if (!Files.isExecutable(adbPath)) {
                log.error("ADB is not executable: {}", adbPath);
                return false;
            }
            config.setAdbPath(adbPath);

            if (!cmd.hasOption("ln")) {
                log.error("Android launcher package name cannot be empty");
                return false;
            }
            config.setAndroidLauncherPkgName(cmd.getOptionValue("ln"));

            if (cmd.hasOption("ag")) {
                config.setTestGenMode(TestGenMode.AUTO);
            } else {
                config.setTestGenMode(TestGenMode.NONE);
            }
        }
        config.setBridgePort(cmd.hasOption("bp") ? Integer.parseInt(cmd.getOptionValue("bp")) : 0);
        return true;
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addRequiredOption("k", "apk-path", true,
                "[Required] Path to APK or a directory contains APK(s)");
        options.addRequiredOption("i", "icc-result-path", true,
                "[Required] Path to ICCBot result directory");
        options.addRequiredOption("c", "testcase-path", true,
                "[Required] Path to testcase directory\n");
        /* options.addRequiredOption("st", "strategy", true,
                "[Required] Strategy used for value provider. Supported strategies include preset, iccBot, random, " +
                        "and randomWithStruct. Use '+' as combination delimiter and ';' as group delimiter. Example: " +
                         "\"iccBot; preset+iccBot+random\"");
         */

        OptionGroup genGroup = new OptionGroup();
        Option autoGenOpt = new Option("ag", "auto-generate", false,
                "Generate testcases for component automatically if not exists");
        Option onlyGenOpt = new Option("og", "only-generate", false,
                "Only generate testcases (without running)");
        Option notGenOpt = new Option("ng", "not-generate", false,
                "Do not generate testcases if not exists\n");
        genGroup.addOption(autoGenOpt);
        genGroup.addOption(onlyGenOpt);
        genGroup.addOption(notGenOpt);
        genGroup.setRequired(true);
        options.addOptionGroup(genGroup);

        options.addOption("ss", "strength-strategy", true,
                "Default comb-strength strategy (0 for dynamic, 1~6 for fixed. Default is 0)");
        options.addOption("o", "scope-config", true,
                "Path to scope config file. Default=null");
        options.addOption("mr", "mist-result", true,
                "Path to MIST result file (in JSON format). Default is null");
        options.addOption("oe", "only-exported", false,
                "Test exported component only. If not specified, default is false");
        options.addOption("rd", "run-in-desc-order", false,
                "Run apks in desc order of path. If not specified, default is asc order\n");

        options.addOption("wp", "with-preset-values", false,
                "Append preset and boundary values when generating SUT. Default is false");
        options.addOption("wr", "with-random-values", false,
                "Append random values when generating SUT. Default is false");
        options.addOption("wm", "with-manifest-values", false,
                "Append values from manifest scope of ICCBot result when generating SUT. Default is false");

        options.addOption("s", "seed", true,
                "Random seed (long). Example: 12345678");
        options.addOption("r", "rand-val-num", true,
                "Number of the random values to generate for each field. Default=5");
        options.addOption("smin", "str-min-length", true,
                "Min length of the random string. Default=" + Constants.DEFAULT_RAND_STR_MIN_LENGTH);
        options.addOption("smax", "str-max-length", true,
                "Max length of the random string. Default=" + Constants.DEFAULT_RAND_STR_MAX_LENGTH + "\n");

        options.addOption("a", "adb-path", true,
                "Path to ADB executable");
        options.addOption("ln", "android-launcher-package-name", true,
                "Android default launcher package name. Example: com.android.launcher3");
        options.addOption("d", "device", true,
                "Serial number of Android device. Default=null");
        options.addOption("bp", "bridge-port", true,
                "Port number of test bridge. Default to auto-detect.");
        options.addOption("ce", "continue-if-error", false,
                "Continue if error occurred when testing components. If not specified, default is false");

        options.addOption("ia", "start-apk-index", true,
                "The start index of apk. Default=0");
        options.addOption("ic", "start-component-index", true,
                "The start index of component. Default=0");
        options.addOption("it", "start-testcase-index", true,
                "The start index of testcase. Default=0");
        options.addOption("is", "start-strategy", true,
                "The name of start strategy. Default is null");

        return options;
    }
}
