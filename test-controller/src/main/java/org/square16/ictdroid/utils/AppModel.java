package org.square16.ictdroid.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
public class AppModel {
    private final Path apkPath;
    private ApkMeta apkMeta;
    private Document apkManifest;
    private JSONObject appModelJson;
    private JSONObject paramSummaryJson;
    private String pkgName;
    private String modelVersion;
    private SortedMap<String, JSONObject> componentsJsonMap;
    private List<JSONObject> componentsJsonList;

    public AppModel(Path apkPath) throws Exception {
        this.apkPath = apkPath;
        loadApk();
        loadICCBotModel();
        log.info("Loaded APP [{}], with {} components in ICCBot component model", pkgName, getCompCount());
    }

    public AppModel(JSONObject appModelJson) {
        this.appModelJson = appModelJson;
        apkPath = null;
        apkMeta = null;
        apkManifest = null;
        paramSummaryJson = null;
        pkgName = appModelJson.getString("package");
        modelVersion = appModelJson.getString("version");
        loadComponents();
        log.info("Loaded APP [{}], with {} components in ICCBot component model", pkgName, getCompCount());
    }

    private void loadApk() throws IOException, ParserConfigurationException, SAXException {
        // Load APK
        try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
            apkMeta = apkFile.getApkMeta();
            if (pkgName != null && !pkgName.equals(apkMeta.getPackageName())) {
                throw new RuntimeException("APK package name not equals to component model package name!");
            } else {
                pkgName = apkMeta.getPackageName();
            }
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            apkManifest = docBuilder.parse(new InputSource(new StringReader(apkFile.getManifestXml())));
        }
    }

    private void loadICCBotModel() throws IOException {
        // Load ICCBot component model
        String apkFileName = apkPath.getFileName().toString();
        String apkFileBaseName = apkFileName.substring(0, apkFileName.lastIndexOf("."));
        Path appModelJsonPath = Paths.get(
                Config.getInstance().getIccResultPath().toString(),
                apkFileBaseName + "/ICCSpecification/ComponentModel.json");
        appModelJson = JSON.parseObject(Files.readString(appModelJsonPath));
        modelVersion = appModelJson.getString("version");

        // Load ICCBot param summary (optional)
        Path paramSummaryJsonPath = Paths.get(
                Config.getInstance().getIccResultPath().toString(),
                apkFileBaseName + "/ICCSpecification/paramSummary.json");
        if (Files.exists(paramSummaryJsonPath) && Files.isReadable(paramSummaryJsonPath)) {
            paramSummaryJson = JSON.parseObject(Files.readString(paramSummaryJsonPath));
            log.info("ICCBot param summary is available!");
        } else {
            paramSummaryJson = null;
        }
        loadComponents();
    }

    private void loadComponents() {
        componentsJsonMap = new TreeMap<>();
        JSONArray compJsonArr = appModelJson.getJSONArray("components");
        if (compJsonArr == null) {
            log.warn("[components] field not found in component model, package=" + pkgName + ", version=" + modelVersion);
        } else {
            List<JSONObject> compArr = compJsonArr.toJavaList(JSONObject.class);
            for (JSONObject comp : compArr) {
                componentsJsonMap.put(comp.getString("className"), comp);
            }
        }
        componentsJsonList = componentsJsonMap.values().stream().toList();

        JSONObject mistResult = Config.getInstance().getMISTResult();
        if (mistResult != null && !mistResult.containsKey(getPackageName())) {
            log.debug("Package [{}] is not in MIST result", getPackageName());
        }
    }

    public JSONObject getParamSummaryJson() {
        return paramSummaryJson;
    }

    public List<String> getAllCompNames() {
        return componentsJsonMap.keySet().stream().toList();
    }

    public String getPackageName() {
        return pkgName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public JSONObject getCompModelJsonByIndex(int index) {
        return componentsJsonList.get(index);
    }

    public CompModel getCompModelByIndex(int index) {
        JSONObject compJson = getCompModelJsonByIndex(index);
        if (compJson == null) {
            return null;
        }
        CompModel compModel = new CompModel(this, compJson);
        ApkManifestUtils.checkCompEnabled(compModel, apkManifest);
        ApkManifestUtils.checkCompExported(compModel, apkManifest);
        MISTUtils.checkCompMISTType(compModel);
        return compModel;
    }

    public int getCompCount() {
        return componentsJsonMap.size();
    }
}
