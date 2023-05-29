package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.Constants;

import java.util.Arrays;

public class FlattenerCategory extends BaseFlattener {
    @Override
    public int flatten(JSONObject valueSet) {
        JSONArray categories = valueSet.getJSONArray("category");
        if (categories == null) {
            return 0;
        }
        for (String category : categories.toJavaList(String.class)) {
            String cName = "category_" + b32encode(category);
            JSONArray cValues = new JSONArray();
            cValues.addAll(Arrays.asList("true", "false"));
            valueSet.put(cName, cValues);
        }
        valueSet.remove("category");

        JSONArray catValues = new JSONArray();
        catValues.addAll(Arrays.asList(Constants.VAL_EMPTY, Constants.VAL_NOT_EMPTY));
        valueSet.put("category", catValues);
        return categories.size();
    }
}
