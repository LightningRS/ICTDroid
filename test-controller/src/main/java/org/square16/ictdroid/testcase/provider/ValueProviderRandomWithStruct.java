package org.square16.ictdroid.testcase.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.testcase.RandomExtraDataGenerator;
import org.square16.ictdroid.testcase.ScopeConfig;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;
import org.square16.ictdroid.utils.GlobalRandom;

import java.util.*;

public class ValueProviderRandomWithStruct extends BaseValueProvider {
    private final JSONObject fullValueSet;
    private final ScopeConfig scopeCfg;
    private final int cntPerField;
    private final int strMinLength;
    private final int strMaxLength;
    private Random random;

    public ValueProviderRandomWithStruct(CompModel compModel, JSONObject fullValueSet,
                                         ScopeConfig scopeCfg, int cntPerField) {
        this(compModel, fullValueSet, scopeCfg, cntPerField,
                Constants.DEFAULT_RAND_STR_MIN_LENGTH, Constants.DEFAULT_RAND_STR_MAX_LENGTH);
    }

    public ValueProviderRandomWithStruct(CompModel compModel, JSONObject fullValueSet,
                                         ScopeConfig scopeCfg, int cntPerField, int strMinLength, int strMaxLength) {
        super();
        this.name = "randomWithStruct";
        this.compModel = compModel;
        this.fullValueSet = fullValueSet;
        this.scopeCfg = scopeCfg;
        this.cntPerField = cntPerField;
        this.strMinLength = strMinLength;
        this.strMaxLength = strMaxLength;
    }

    @Override
    public JSONObject getValueSet() {
        if (!Config.getInstance().getWithRandom()) {
            return null;
        }
        this.random = GlobalRandom.getInstance();
        BaseValueProvider randomProvider = new ValueProviderRandom(compModel, this.cntPerField);
        JSONObject valueSet = randomProvider.getValueSet();
        if (fullValueSet == null) {
            return valueSet;
        }

        // Merge extra structure
        JSONObject extraData = fullValueSet.getJSONObject("extras");
        if (extraData != null) {
            // Deep copy
            extraData = JSON.parseObject(extraData.toJSONString());
            JSONArray mergedValues = new JSONArray();
            for (String scopeName : extraData.keySet()) {
                if (!scopeCfg.getScopeConfig("extra", scopeName)) {
                    continue;
                }
                mergeCompJSONArrRecur(mergedValues, extraData.getJSONArray(scopeName));
            }
            randomizeExtraValuesRecur(null, mergedValues);
            valueSet.put("extra", mergedValues);
        }
        return valueSet;
    }

    private void randomizeExtraValuesRecur(Object parent, Object root) {
        if (root instanceof JSONObject rootObj) {
            // If there is no "values" attr in an extra object,
            // we should put a temporary empty JSONArray placeholder.
            JSONArray values = rootObj.getJSONArray("values");
            if (rootObj.getString("type") != null && values == null) {
                JSONArray placeholder = new JSONArray();
                placeholder.add("new " + rootObj.getString("type"));
                rootObj.put("values", placeholder);
            }
            for (String name : rootObj.keySet()) {
                randomizeExtraValuesRecur(rootObj, rootObj.get(name));
            }
        } else if (root instanceof JSONArray jsonArr) {
            if (jsonArr.size() == 0) {
                return;
            }
            Object firstObj = jsonArr.get(0);
            if (firstObj instanceof String) {
                jsonArr.clear();
                if (parent instanceof JSONObject parentJsonObj) {
                    String type = parentJsonObj.getString("type");
                    if (type != null) {
                        type = type.replaceAll("java\\.lang\\.", "");
                        if ("boolean".equals(type) || "Boolean".equals(type)) {
                            jsonArr.addAll(Arrays.asList("true", "false"));
                        } else {
                            Collection<String> genValues = generateRandomValues(type, cntPerField);
                            jsonArr.addAll(genValues);
                        }
                    }
                }
            } else {
                for (Object obj : jsonArr) {
                    randomizeExtraValuesRecur(root, obj);
                }
            }
        }
    }

    private Collection<String> generateRandomValues(String type, int cnt) {
        Set<String> genValues = new HashSet<>();
        while (genValues.size() < cnt) {
            String value = RandomExtraDataGenerator.generate(type, this.strMinLength, this.strMaxLength, this.random);
            if (value == null) {
                genValues.add("new " + type);
                break;
            } else {
                genValues.add(value);
            }
        }
        return genValues;
    }
}
