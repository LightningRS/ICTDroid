package org.square16.ictdroid.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@AllArgsConstructor
public class CompModel {
    public static final String TYPE_ACTIVITY = "a";
    public static final String TYPE_SERVICE = "s";
    public static final String TYPE_RECEIVER = "r";

    private AppModel appModel;
    private JSONObject compJson;

    /**
     * Whether component is enabled.
     * Will be null if not determined.
     */
    @Nullable
    private Boolean enabled;

    /**
     * Whether component is exported.
     * Will be null if not determined.
     */
    @Nullable
    private Boolean exported;

    /**
     * Component type in MIST result.
     * Will be null if not determined.
     */
    @Nullable
    private String mistType;

    public CompModel(AppModel appModel, JSONObject compJson) {
        this(appModel, compJson, null, null, null);
    }

    public String getPackageName() {
        return appModel.getPackageName();
    }

    public String getClassName() {
        return compJson.getString("className");
    }

    public String getType() {
        return compJson.getString("type");
    }

    @Nullable
    public JSONObject getFullValueSet() {
        return compJson.getJSONObject("fullValueSet");
    }

    public boolean hasFieldScopeValues(String scopeName, String... fieldNames) {
        JSONObject fieldValueSet = getFullValueSet();
        if (fieldValueSet == null) {
            return false;
        }
        for (String fieldName : fieldNames) {
            fieldValueSet = fieldValueSet.getJSONObject(ICCBotUtils.getModelFieldName(fieldName));
            if (fieldValueSet == null) {
                return false;
            }
        }
        return fieldValueSet.containsKey(scopeName);
    }
}
