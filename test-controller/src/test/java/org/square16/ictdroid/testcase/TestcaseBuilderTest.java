package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.testcase.provider.*;
import org.square16.ictdroid.utils.AppModel;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestcaseBuilderTest {
    @Test
    void testBuilder2() {
        Path testModel = Paths.get("src", "test", "resources", "testComponentModel2.json").toAbsolutePath();
        Path outputCSV = Paths.get("src", "test", "resources", "test2.csv").toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            JSONObject compObj = compsArr.getJSONObject(39);
            CompModel compModel = new CompModel(new AppModel(modelObj), compObj);
            String pkgName = modelObj.getString("package");
            String compName = compModel.getClassName();
            BaseTestcaseBuilder builder = new ACTSTestcaseBuilder(compModel);

            JSONObject presetValues = new ValueProviderPreset(compModel).getValueSet();
            assertNotNull(presetValues);

            BaseValueProvider iccBotProvider = new ValueProviderICCBot(
                    compModel, compObj.getJSONObject("fullValueSet"), builder.getScopeConfig());
            builder.addValueSet(iccBotProvider.getValueSet());
            builder.addValueSet(presetValues);
            builder.build(outputCSV.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testBuilder() {
        Path testModel = Paths.get("src", "test", "resources", "testComponentModel.json").toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            for (JSONObject compObj : compsArr.toJavaList(JSONObject.class)) {
                CompModel compModel = new CompModel(new AppModel(modelObj), compObj);
                Path outputCSV = Paths.get("src", "test", "resources", compObj.getString("className") + ".csv").toAbsolutePath();
                BaseTestcaseBuilder builder = new ACTSTestcaseBuilder(compModel);

                String pkgName = modelObj.getString("package");
                String compName = compModel.getClassName();

                JSONObject presetValues = new ValueProviderPreset(compModel).getValueSet();
                assertNotNull(presetValues);

                JSONObject iccBotValueSet = new ValueProviderICCBot(
                        compModel, compObj.getJSONObject("fullValueSet"), builder.getScopeConfig()).getValueSet();
                if (iccBotValueSet != null) builder.addValueSet(iccBotValueSet);
                builder.addValueSet(presetValues);
                builder.build(outputCSV.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testBuilder3() {
        Path testModel = Paths.get("src", "test", "resources", "testComponentModel2.json").toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            for (JSONObject compObj : compsArr.toJavaList(JSONObject.class)) {
                CompModel compModel = new CompModel(new AppModel(modelObj), compObj);
                Path outputCSV = Paths.get("src", "test", "resources", compObj.getString("className") + ".csv").toAbsolutePath();
                BaseTestcaseBuilder builder = new ACTSTestcaseBuilder(compModel);

                String pkgName = modelObj.getString("package");
                String compName = compModel.getClassName();

                JSONObject presetValues = new ValueProviderPreset(compModel).getValueSet();
                assertNotNull(presetValues);

                JSONObject iccBotValueSet = new ValueProviderICCBot(
                        compModel, compObj.getJSONObject("fullValueSet"), builder.getScopeConfig()).getValueSet();
                if (iccBotValueSet != null) builder.addValueSet(iccBotValueSet);
                builder.addValueSet(presetValues);
                builder.build(outputCSV.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void test4strategies1() {
        buildWithAll4Strategies("testComponentModel.json");
        buildWithAll4Strategies("testComponentModel2.json");
    }

    void buildWithAll4Strategies(String modelName) {
        Path testModel = Paths.get("src", "test", "resources", modelName).toAbsolutePath();
        assertTrue(Files.exists(testModel));
        Config.getInstance().setSeed(12345678L);
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            for (JSONObject compObj : compsArr.toJavaList(JSONObject.class)) {
                CompModel compModel = new CompModel(new AppModel(modelObj), compObj);
                Path outputCSV = Paths.get("src", "test", "resources", "all4strategies", compObj.getString("className") + ".csv").toAbsolutePath();
                BaseTestcaseBuilder builder = new ACTSTestcaseBuilder(compModel);

                String pkgName = modelObj.getString("package");
                String compName = compModel.getClassName();

                JSONObject presetValues = new ValueProviderPreset(compModel).getValueSet();
                assertNotNull(presetValues);

                JSONObject iccBotValueSet = new ValueProviderICCBot(compModel, compObj.getJSONObject("fullValueSet"), builder.getScopeConfig()).getValueSet();
                JSONObject randValueSet = new ValueProviderRandom(compModel, 5).getValueSet();
                JSONObject randWithStructValueSet = new ValueProviderRandomWithStruct(
                        compModel, compObj.getJSONObject("fullValueSet"), builder.getScopeConfig(), 5).getValueSet();
                if (iccBotValueSet != null) builder.addValueSet(iccBotValueSet);
                if (randWithStructValueSet != null) builder.addValueSet(randWithStructValueSet);
                builder.addValueSet(randValueSet);
                builder.addValueSet(presetValues);
                builder.build(outputCSV.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
