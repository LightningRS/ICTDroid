package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.utils.Config;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class FlattenerExtra extends BaseFlattener {
    private static int nodeId;

    public static String encodeExtraName(String name) {
        return b32encode(name);
    }

    public static String encodeExtraNameAndType(String name, String type) {
        if (type.isBlank() || name.isBlank()) {
            log.error("Extra name and type cannot be empty! name={}, type={}", name, type);
            return null;
        }
        type = type.replaceAll("java\\.lang\\.", "");
        if (type.contains("@")) {
            type = type.substring(0, type.indexOf("@"));
        }
        return b32encode(name) + "_" + b32encode(type);
    }

    @Nullable
    public static String encodeExtraNameAndType(String nameAndType) {
        if (!nameAndType.contains("-")) {
            log.error("Invalid extra nameAndType format: {}", nameAndType);
            return null;
        }
        String[] sp = nameAndType.split("-", 2);
        String type = sp[0].trim(), name = sp[1].trim();
        return encodeExtraNameAndType(name, type);
    }

    private static void appendNullAndEmpty(Set<String> fValues, String extraType) {
        if ("boolean".equalsIgnoreCase(extraType)) {
            fValues.add("true");
            fValues.add("false");
        } else if ("char".equalsIgnoreCase(extraType)
                || "byte".equalsIgnoreCase(extraType)
                || "short".equalsIgnoreCase(extraType)
                || "int".equalsIgnoreCase(extraType)
                || "long".equalsIgnoreCase(extraType)
                || "float".equalsIgnoreCase(extraType)
                || "double".equalsIgnoreCase(extraType)) {
            fValues.add("0");
            fValues.add("1");
        } else {
            boolean isParcelSerial = extraType.contains("Parcelable") || extraType.contains("Serializable");
            fValues.add(Constants.VAL_NULL);

            if (!isParcelSerial) fValues.add(Constants.VAL_EMPTY);

            if (extraType.endsWith("ArrayList")) {
                fValues.add(Constants.VAL_NOT_EMPTY_ARR_NULL_ELEM);
                if (!isParcelSerial) fValues.add(Constants.VAL_NOT_EMPTY_ARR_EMPTY_ELEM);
                fValues.add(Constants.VAL_NOT_EMPTY_ARR_NOT_EMPTY_ELEM);
            } else {
                fValues.add(Constants.VAL_NOT_EMPTY);
            }
        }
    }

    private static void appendBoundaryValues(Set<String> fValues, String extraType) {
        switch (extraType.toLowerCase()) {
            case "boolean" -> fValues.addAll(Arrays.asList("true", "false"));
            case "char" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Character.MIN_VALUE),
                    String.valueOf(Character.MAX_VALUE)
            ));
            case "byte" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Byte.MIN_VALUE),
                    String.valueOf(Byte.MAX_VALUE),
                    "0"
            ));
            case "short" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Short.MIN_VALUE),
                    String.valueOf(Short.MAX_VALUE),
                    "0"
            ));
            case "int" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Integer.MAX_VALUE),
                    String.valueOf(Integer.MIN_VALUE),
                    "0"
            ));
            case "long" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Long.MAX_VALUE),
                    String.valueOf(Long.MIN_VALUE),
                    "0"
            ));
            case "float" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Float.MAX_VALUE),
                    String.valueOf(Float.MIN_VALUE),
                    "0"
            ));
            case "double" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Double.MAX_VALUE),
                    String.valueOf(Double.MIN_VALUE),
                    "0"
            ));
            default -> {
            }
        }
    }

    @Override
    public int flatten(JSONObject valueSet) {
        JSONArray extraArr = valueSet.getJSONArray("extra");
        JSONArray extraBaseValues = new JSONArray();
        extraBaseValues.add(Constants.VAL_NULL);
        extraBaseValues.add(Constants.VAL_EMPTY);
        if (extraArr == null) {
            valueSet.put("extra", extraBaseValues);
            return 0;
        }
        nodeId = 0;
        int sum = 0;
        for (JSONObject extraObj : extraArr.toJavaList(JSONObject.class)) {
            sum += flattenRecur(valueSet, extraObj, 0);
        }
        extraBaseValues.add(Constants.VAL_NOT_EMPTY);
        valueSet.put("extra", extraBaseValues);
        return sum;
    }

    private int flattenRecur(JSONObject valueSet, JSONObject extraObj, int parentId) {
        nodeId++;
        int thisNodeId = nodeId;
        String extraName = extraObj.getString("name");
        String extraType = extraObj.getString("type").replaceAll("java\\.lang\\.", "");
        if ("bundle".equals(extraType)) {
            extraType = "Bundle";
        } else if (extraType.endsWith("[]")) {
            extraType = extraType.replace("[]", "Array");
        } else if ("Object".equals(extraType)) {
            extraType = "Parcelable";
        }
        String extraNodeName = "extra_" + parentId + "_" + nodeId + "_" + encodeExtraNameAndType(extraName, extraType);
        Set<String> fValues = new HashSet<>();
        JSONArray bodyObj = extraObj.getJSONArray("body");
        if (bodyObj == null) {
            // Leaf node
            JSONArray values = extraObj.getJSONArray("values");
            if (values != null) {
                for (String value : values.toJavaList(String.class)) {
                    if ("long".equalsIgnoreCase(extraType) && value.endsWith("L")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if ("boolean".equalsIgnoreCase(extraType)) {
                        fValues.add("1".equals(value) ? "true" : "false");
                    } else if (value.startsWith("new ")) {
                        log.debug("new (constructor) detected in value set, ignored");
                    } else {
                        fValues.add(value);
                    }
                }
            }
            appendNullAndEmpty(fValues, extraType);
            if (Config.getInstance().getWithPresetAndBoundary()) {
                appendBoundaryValues(fValues, extraType);
            }
            valueSet.put(extraNodeName, fValues);
            return 1;
        } else {
            fValues.addAll(Arrays.asList(Constants.VAL_NULL, Constants.VAL_EMPTY, Constants.VAL_NOT_EMPTY));
            valueSet.put(extraNodeName, fValues);
            int sum = 0;
            for (JSONObject subObj : bodyObj.toJavaList(JSONObject.class)) {
                sum += flattenRecur(valueSet, subObj, thisNodeId);
            }
            return 1 + sum;
        }
    }
}
