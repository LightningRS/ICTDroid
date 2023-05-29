package org.square16.ictdroid.testcase.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.testcase.ScopeConfig;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;
import org.square16.ictdroid.utils.ICCBotUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ValueProviderICCBot extends BaseValueProvider {
    private static Set<String> IGNORE_VALUES = new HashSet<>() {{
        addAll(Arrays.asList("mHost", "mPort", "mPath", "mSheme", "mScheme", "mAuthority"));
    }};
    private final JSONObject fullValueSet;
    private final ScopeConfig scopeCfg;

    public ValueProviderICCBot(CompModel compModel, JSONObject fullValueSet, ScopeConfig scopeCfg) {
        super();
        this.name = "iccBot";
        this.compModel = compModel;
        this.fullValueSet = fullValueSet;
        this.scopeCfg = new ScopeConfig(scopeCfg);
    }

    protected void updateCompScopeCfg() {
        if (Config.getInstance().getScopeConfigPath() != null) {
            // User defined scope config, ignore updating
            log.info("updateCompScopeCfg: User-defined scope config detected, ignore updating scope config!");
            return;
        }
        if (Constants.MIST_TYPE_MUST_IA.equals(compModel.getMistType())) {
            log.info("updateCompScopeCfg: [{}/{}] is mustIA according to MIST, only consider values in send/recv scope",
                    getPackageName(), getCompName());
            scopeCfg.replaceScopeConfig("manifest", false);
        } else if (Config.getInstance().getWithManifest()) {
            log.info("updateCompScopeCfg: Append manifest scope values (exclude category)");
            scopeCfg.replaceScopeConfig("manifest", true);
            // Exclude category
            scopeCfg.setFieldScopeConfig("category", "manifest", false);
        }
    }

    @Override
    public JSONObject getValueSet() {
        if (fullValueSet == null) {
            return null;
        }
        JSONObject mergedValueSetObj = new JSONObject();
        updateCompScopeCfg();
        JSONObject fullValueSetDup = JSON.parseObject(fullValueSet.toJSONString());
        for (String fieldName : fullValueSetDup.keySet()) {
            String realFieldName = ICCBotUtils.getRealFieldName(fieldName);
            JSONObject fieldValues = fullValueSetDup.getJSONObject(fieldName);

            if ("data".equals(realFieldName)) {
                Set<String> hostsTemp = new HashSet<>();
                Set<String> portsTemp = new HashSet<>();
                Set<String> authorityTemp = new HashSet<>();
                for (String dataFieldName : fieldValues.keySet()) {
                    JSONObject dataFieldValues = fieldValues.getJSONObject(dataFieldName);
                    String realDataFieldName = ICCBotUtils.getRealFieldName(dataFieldName);
                    JSONArray mergedValues = new JSONArray();
                    for (String scopeName : dataFieldValues.keySet()) {
                        if (!scopeCfg.getScopeConfig("data", scopeName)) {
                            continue;
                        }
                        if ("host".equals(realDataFieldName)) {
                            hostsTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class)
                                    .stream().filter(v -> !v.isBlank() && !IGNORE_VALUES.contains(v)).distinct().toList());
                        } else if ("port".equals(realDataFieldName)) {
                            portsTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class)
                                    .stream().filter(v -> !v.isBlank() && !IGNORE_VALUES.contains(v)).distinct().toList());
                        } else if ("authority".equals(realDataFieldName)) {
                            authorityTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class)
                                    .stream().filter(v -> !v.isBlank() && !IGNORE_VALUES.contains(v)).distinct().toList());
                        } else {
                            mergedValues.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class)
                                    .stream().filter(v -> !v.isBlank() && !IGNORE_VALUES.contains(v)).distinct().toList());
                        }
                    }
                    if (!"host".equals(realDataFieldName)
                            && !"port".equals(realDataFieldName)
                            && !"authority".equals(realDataFieldName)) {
                        mergedValueSetObj.put(realDataFieldName, mergedValues);
                    }
                }
                for (String host : hostsTemp) {
                    for (String port : portsTemp) {
                        authorityTemp.add(String.format("%s:%s", host, port));
                    }
                }
                authorityTemp.addAll(hostsTemp);
                authorityTemp.addAll(portsTemp);
                JSONArray authorityArr = new JSONArray();
                authorityArr.addAll(authorityTemp);
                mergedValueSetObj.put("authority", authorityArr);
            } else {
                JSONArray mergedValues = new JSONArray();
                for (String scopeName : fieldValues.keySet()) {
                    if (!scopeCfg.getScopeConfig(realFieldName, scopeName)) {
                        continue;
                    }
                    mergeCompJSONArrRecur(mergedValues, fieldValues.getJSONArray(scopeName));
                }
                mergedValueSetObj.put(realFieldName, mergedValues);
            }
        }
        return mergedValueSetObj;
    }
}
