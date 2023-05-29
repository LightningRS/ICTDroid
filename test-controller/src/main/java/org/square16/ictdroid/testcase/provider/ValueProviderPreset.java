package org.square16.ictdroid.testcase.provider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;
import org.square16.ictdroid.utils.ICCBotUtils;
import org.square16.ictdroid.utils.MISTUtils;

import java.util.*;

/**
 * @author Zsx
 */
@Slf4j
public class ValueProviderPreset extends BaseValueProvider {
    public ValueProviderPreset(CompModel compModel) {
        super();
        this.name = "preset";
        this.compModel = compModel;
    }

    /**
     * <h3>About PRESET values:
     *
     * <p>For all preset values of each field defined as <i>Constants.PRESET_VALUES_XXX</i>, we
     * ensure that at least <b>the first one</b> is an <i>INVALID</i> value, and <b>the second one</b>
     * is a <i>VALID</i> value.
     * <p>For those fields with collected values under send/recv scope in the component model, since
     * they <b>have been truly used in program code</b>, we put all pre-defined preset values into the
     * combination testing model, while only one <i>INVALID</i> and one <i>VALID</i> values
     * will be used otherwise.
     *
     * @return Preset value set as JSONObject
     **/
    @Override
    public JSONObject getValueSet() {
        if (!Config.getInstance().getWithPresetAndBoundary()) {
            return null;
        }
        JSONObject res = new JSONObject();

        JSONArray categoryArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "category")
                || compModel.hasFieldScopeValues("recvIntent", "category")) {
            categoryArr.addAll(Arrays.asList(Constants.PRESET_VALUES_CATEGORY));
        } else {
            categoryArr.addAll(Arrays.asList(Constants.PRESET_VALUES_CATEGORY).subList(0, 2));
        }
        res.put("category", categoryArr);

        JSONArray typeArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "type")
                || compModel.hasFieldScopeValues("recvIntent", "type")) {
            typeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_TYPE));
        } else {
            typeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_TYPE).subList(0, 2));
        }
        res.put("type", typeArr);

        boolean isDataUsed = MISTUtils.isDataUsed(compModel);

        JSONArray schemeArr = new JSONArray();
        if (isDataUsed) {
            schemeArr.add(Constants.PRESET_VALUES_SCHEME[0]);
            String presetVal = getDataFieldAnyNotPresentValue("scheme", Constants.PRESET_VALUES_SCHEME);
            if (presetVal != null) {
                schemeArr.add(presetVal);
            }
        } else {
            schemeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_SCHEME).subList(0, 2));
        }
        res.put("scheme", schemeArr);

        JSONArray authorityArr = new JSONArray();
        if (isDataUsed) {
            authorityArr.add(Constants.PRESET_VALUES_AUTHORITY[0]);
            String presetVal = getDataFieldAnyNotPresentValue("authority", Constants.PRESET_VALUES_AUTHORITY);
            if (presetVal != null) {
                authorityArr.add(presetVal);
            }
        } else {
            authorityArr.addAll(Arrays.asList(Constants.PRESET_VALUES_AUTHORITY).subList(0, 2));
        }
        res.put("authority", authorityArr);

        JSONArray pathArr = new JSONArray();
        if (isDataUsed) {
            pathArr.add(Constants.PRESET_VALUES_PATH[0]);
            String presetVal = getDataFieldAnyNotPresentValue("path", Constants.PRESET_VALUES_PATH);
            if (presetVal != null) {
                pathArr.add(presetVal);
            }
        } else {
            pathArr.addAll(Arrays.asList(Constants.PRESET_VALUES_PATH).subList(0, 2));
        }
        res.put("path", pathArr);
        return res;
    }

    private String getDataFieldAnyNotPresentValue(@NonNull String dataFieldName, @NonNull String[] presetValues) {
        Set<String> existValues = new HashSet<>();
        try {
            JSONObject fullValueSet = compModel.getFullValueSet();
            JSONObject dataValueSet = Objects.requireNonNull(fullValueSet)
                    .getJSONObject(ICCBotUtils.getModelFieldName("data"));
            if (!"authority".equals(dataFieldName) || dataValueSet.containsKey("authority")) {
                JSONObject fieldValueSet = Objects.requireNonNull(fullValueSet)
                        .getJSONObject(ICCBotUtils.getModelFieldName("data"))
                        .getJSONObject(ICCBotUtils.getModelFieldName(dataFieldName));
                fieldValueSet.values().forEach(jsonArr -> existValues.addAll(((JSONArray) jsonArr).toJavaList(String.class)));
            }
            if ("authority".equals(dataFieldName)) {
                JSONObject hostValueSet = Objects.requireNonNull(fullValueSet)
                        .getJSONObject(ICCBotUtils.getModelFieldName("data"))
                        .getJSONObject(ICCBotUtils.getModelFieldName("host"));
                JSONObject portValueSet = Objects.requireNonNull(fullValueSet)
                        .getJSONObject(ICCBotUtils.getModelFieldName("data"))
                        .getJSONObject(ICCBotUtils.getModelFieldName("port"));
                Set<String> hostValues = new HashSet<>();
                Set<String> portValues = new HashSet<>();
                hostValueSet.values().forEach(hostArr -> hostValues.addAll(((JSONArray) hostArr).toJavaList(String.class)));
                portValueSet.values().forEach(portArr -> portValues.addAll(((JSONArray) portArr).toJavaList(String.class)));
                hostValues.forEach(host -> {
                    portValues.forEach(port -> {
                        String hostStr = host.trim();
                        String portStr = port.trim();
                        if (!hostStr.isBlank() && !portStr.isBlank()) {
                            existValues.add(String.format("%s:%s", hostStr, portStr));
                        } else if (!hostStr.isBlank() && portStr.isBlank()) {
                            existValues.add(hostStr);
                        } else if (hostStr.isBlank() && !portStr.isBlank()) {
                            existValues.add(portStr);
                        }
                    });
                    existValues.add(host.trim());
                });
            }

            Optional<String> presetVal = Arrays.asList(presetValues)
                    .subList(1, presetValues.length)
                    .stream().filter(s -> !existValues.contains(s)).findAny();
            if (presetVal.isPresent()) {
                log.debug("Got one {} preset value: {}", dataFieldName, presetVal.get());
                return presetVal.get();
            } else {
                log.warn("No preset value got for field {}", dataFieldName);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
