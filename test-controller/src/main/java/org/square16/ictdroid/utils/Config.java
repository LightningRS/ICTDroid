package org.square16.ictdroid.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.nio.file.Path;

@Data
public class Config {
    private Path adbPath;
    private String androidLauncherPkgName;
    private Path apkPath;
    private boolean isApkPathDirectory;
    private Path iccResultPath;
    private Path testcasePath;
    private Path scopeConfigPath;
    private String deviceSerial;
    private Long seed;
    private TestGenMode testGenMode;
    private int randValNum;
    private int strMinLength;
    private int strMaxLength;
    private String strategy;
    private Boolean continueIfError;
    private Boolean onlyExported;
    private Boolean runApksInDescOrder;
    private Integer startApkIndex;
    private Integer startCompIndex;
    private Integer startCaseIndex;
    private String startStrategy;
    private int defaultStrength;
    private Boolean withManifest;

    // ========= Value Provider Options =========
    private Boolean withRandom;
    private Boolean withPresetAndBoundary;
    private JSONObject MISTResult;

    private Config() {

    }

    public static Config getInstance() {
        return ConfigHolder.INSTANCE;
    }

    private static class ConfigHolder {
        private static final Config INSTANCE = new Config();
    }
}
