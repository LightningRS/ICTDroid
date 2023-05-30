package org.square16.ictdroid.testbridge.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import com.opencsv.CSVReader;
import org.square16.ictdroid.testbridge.Constants;

import org.apache.commons.codec.binary.Base32;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSVTestCaseMgr {
    private static final String TAG = "CSVTestCaseUtil";
    private final String pkgName;
    private final String compName;
    private final String strategy;
    private final String rootPath;
    private String[] fieldNames;
    private final ArrayList<String[]> cases = new ArrayList<>();

    public CSVTestCaseMgr(String pkgName, String compName, String strategy) throws FileNotFoundException {
        this(pkgName, compName, strategy, "/storage/emulated/0/ICTDroid/testcases");
    }

    public CSVTestCaseMgr(String pkgName, String compName, String strategy, String rootPath) throws FileNotFoundException {
        this.pkgName = pkgName;
        this.compName = compName;
        this.strategy = strategy;
        this.rootPath = rootPath;
        this.init();
    }

    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

    public void addTestcase(String[] testcase) {
        this.cases.add(testcase);
    }

    private void init() throws FileNotFoundException {
        File configFile = new File(rootPath + "/" + this.pkgName, this.compName + "_" + this.strategy + ".csv");
        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));
        try {
            reader.skip(6);
            Iterator<String[]> it = reader.iterator();
            int i = 0;
            while (it.hasNext()) {
                String[] cols = it.next();
                if (i == 0) this.fieldNames = cols;
                else this.cases.add(cols);
                i++;
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException when loading testcases");
            e.printStackTrace();
        }
        Log.i(TAG, "Loaded " + this.cases.size() + " testcases");
    }

    public int getSize() {
        return this.cases.size();
    }

    private Object getFieldValue(String s, String type) {
        switch (s) {
            case Constants.VAL_NULL:
                // null for Object
                return null;

            case Constants.VAL_EMPTY:
                // Empty value for Object
                switch (type) {
                    case "String":
                    case "CharSequence":
                        return "";

                    // Array
                    case "byteArray":
                        return new byte[0];
                    case "shortArray":
                        return new short[0];
                    case "intArray":
                        return new int[0];
                    case "longArray":
                        return new long[0];
                    case "doubleArray":
                        return new double[0];
                    case "booleanArray":
                        return new boolean[0];
                    case "floatArray":
                        return new float[0];
                    case "charArray":
                        return new char[0];
                    case "ParcelableArray":
                        return new Parcelable[0];
                    case "StringArray":
                        return new String[0];
                    case "CharSequenceArray":
                        return new CharSequence[0];

                    // ArrayList
                    case "IntegerArrayList":
                        return new ArrayList<Integer>();
                    case "ParcelableArrayList":
                        return new ArrayList<Parcelable>();
                    case "CharSequenceArrayList":
                    case "StringArrayList":
                        return new ArrayList<String>();

                    case "Bundle":
                        return new Bundle();
                    case "Serializable":
                        return new TestSerializable();
                    case "Parcelable":
                        return new TestParcelable();
                    default:
                        Log.e(TAG, "Unsupported type for empty value: " + type);
                        return null;
                }
            case Constants.VAL_NOT_EMPTY:
                // notEmpty value for Object
                switch (type) {
                    // Array
                    case "byteArray":
                        return new byte[]{0};
                    case "shortArray":
                        return new short[]{0};
                    case "intArray":
                        return new int[]{0};
                    case "longArray":
                        return new long[]{0};
                    case "doubleArray":
                        return new double[]{0.0};
                    case "booleanArray":
                        return new boolean[]{true};
                    case "floatArray":
                        return new float[]{0.0f};
                    case "charArray":
                        return new char[]{'a'};
                    case "ParcelableArray":
                        return new Parcelable[]{ParcelUuid.fromString(Constants.DEFAULT_PARCELABLE_UUID)};
                    case "StringArray":
                    case "CharSequenceArray":
                        return new String[]{Constants.DEFAULT_NOT_EMPTY_STR};

                    case "String":
                    case "CharSequence":
                        return Constants.DEFAULT_NOT_EMPTY_STR;

                    case "Serializable":
                        return Constants.DEFAULT_SERIALIZABLE_STR;

                    case "Parcelable":
                        return ParcelUuid.fromString(Constants.DEFAULT_PARCELABLE_UUID);

                    // ArrayList (old strategy)
                    case "IntegerArrayList":
                        return new ArrayList<Integer>() {{
                            add(1);
                        }};
                    case "ParcelableArrayList":
                        return new ArrayList<Parcelable>() {{
                            add(ParcelUuid.fromString(Constants.DEFAULT_PARCELABLE_UUID));
                        }};
                    case "CharSequenceArrayList":
                    case "StringArrayList":
                        return new ArrayList<CharSequence>() {{
                            add(Constants.DEFAULT_NOT_EMPTY_STR);
                        }};

                    case "Bundle":
                        Bundle bd = new Bundle();
                        bd.putString("TestString", "TestString");
                        return bd;
                    default:
                        Log.e(TAG, "Unsupported type for notEmpty value: " + type);
                        return null;
                }
            case Constants.VAL_NOT_EMPTY_ARR_NULL_ELEM:
                switch (type) {
                    // ArrayList
                    case "IntegerArrayList":
                        return new ArrayList<Integer>() {{
                            add(null);
                        }};
                    case "ParcelableArrayList":
                        return new ArrayList<Parcelable>() {{
                            add(null);
                        }};
                    case "CharSequenceArrayList":
                    case "StringArrayList":
                        return new ArrayList<CharSequence>() {{
                            add(null);
                        }};
                    default:
                        Log.e(TAG, "Unsupported type for notEmpty_null value: " + type);
                        return null;
                }
            case Constants.VAL_NOT_EMPTY_ARR_EMPTY_ELEM:
                switch (type) {
                    // ArrayList
                    case "IntegerArrayList":
                        return new ArrayList<Integer>() {{
                            add(0);
                        }};
                    case "ParcelableArrayList":
                        return new ArrayList<Parcelable>() {{
                            add(new TestParcelable());
                        }};
                    case "CharSequenceArrayList":
                    case "StringArrayList":
                        return new ArrayList<CharSequence>() {{
                            add("");
                        }};
                    default:
                        Log.e(TAG, "Unsupported type for notEmpty_empty value: " + type);
                        return null;
                }
            case Constants.VAL_NOT_EMPTY_ARR_NOT_EMPTY_ELEM:
                switch (type) {
                    // ArrayList
                    case "IntegerArrayList":
                        return new ArrayList<Integer>() {{
                            add(1);
                        }};
                    case "ParcelableArrayList":
                        return new ArrayList<Parcelable>() {{
                            add(ParcelUuid.fromString(Constants.DEFAULT_PARCELABLE_UUID));
                        }};
                    case "CharSequenceArrayList":
                    case "StringArrayList":
                        return new ArrayList<CharSequence>() {{
                            add(Constants.DEFAULT_NOT_EMPTY_STR);
                        }};
                    default:
                        Log.e(TAG, "Unsupported type for notEmpty_notEmpty value: " + type);
                        return null;
                }
            default:
                // Normal value for Object
                try {
                    switch (type) {
                        case "byte":
                        case "Byte":
                            return Byte.parseByte(s);
                        case "short":
                        case "Short":
                            return Short.parseShort(s);
                        case "int":
                        case "Integer":
                            return Integer.parseInt(s);
                        case "float":
                        case "Float":
                            return Float.parseFloat(s);
                        case "double":
                        case "Double":
                            return Double.parseDouble(s);
                        case "long":
                        case "Long":
                            return Long.parseLong(s);
                        case "boolean":
                        case "Boolean":
                            return Boolean.parseBoolean(s);
                        case "char":
                            return (char) Integer.parseInt(s);
                        case "String":
                        case "CharSequence":
                            return s;
                        default:
                            Log.e(TAG, "Unsupported value [" + s + "] for type [" + type + "]");
                            return null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception when converting value [" + s + "] for type [" + type + "]", e);
                    return null;
                }
                // End switch
        }
    }

    public Intent getTestCaseIntent(int caseId) {
        String[] caseCols = this.cases.get(caseId);
        Intent i = new Intent();
        assert caseCols.length == this.fieldNames.length;
        boolean hasData = false;
        boolean hasExtra = false;
        boolean hasCategory = false;
        Map<String, String> unprocessed = new HashMap<>();

        for (int idx = 0; idx < this.fieldNames.length; idx++) {
            String fieldName = this.fieldNames[idx];
            String fieldValStr = caseCols[idx];
            Object fieldVal;

            switch (fieldName) {
                case "action":
                    i.setAction((String) this.getFieldValue(fieldValStr, "String"));
                    break;
                case "category":
                    if (Constants.VAL_NOT_EMPTY.equals(fieldValStr)) {
                        hasCategory = true;
                    } else {
                        i.addCategory((String) this.getFieldValue(fieldValStr, "String"));
                    }
                    break;
                case "type":
                    // When calling setType, data will be set to null.
                    // So we must save the data field and recovery it.
                    Uri oldData = null;
                    if (i.getData() != null) {
                        oldData = i.getData();
                    }
                    i.setType((String) this.getFieldValue(fieldValStr, "String"));
                    if (oldData != null) {
                        i.setData(oldData);
                    }
                    break;
                case "flag":
                    fieldVal = this.getFieldValue(fieldValStr, "Integer");
                    if (fieldVal != null) {
                        i.setFlags((Integer) fieldVal);
                    }
                    break;
                case "data":
                    if (Constants.VAL_NOT_EMPTY.equals(fieldValStr)) {
                        hasData = true;
                    } else if (Constants.VAL_EMPTY.equals(fieldValStr)){
                        i.setData(Uri.parse(""));
                    } // data=null means do not set data
                    break;
                case "extra":
                    if (Constants.VAL_NOT_EMPTY.equals(fieldValStr)) {
                        hasExtra = true;
                    } else if (Constants.VAL_EMPTY.equals(fieldValStr)) {
                        i.putExtras((Bundle) this.getFieldValue(fieldValStr, "Bundle"));
                    }  // extra=null means do not set extra
                    break;
                default:
                    unprocessed.put(fieldName, fieldValStr);
            }
        }

        // Parse data
        String scheme = unprocessed.remove("scheme");
        String authority = unprocessed.remove("authority");
        String path = unprocessed.remove("path");
        if (hasData) {
            scheme = scheme != null ? (String) this.getFieldValue(scheme, "String") : "";
            authority = authority != null ? (String) this.getFieldValue(authority, "String") : "";
            path = path != null ? (String) this.getFieldValue(path, "String") : "";
            Uri uri = Uri.parse(scheme + ":" + authority + path);
            i.setData(uri);
        }

        // Parse category
        List<String> proceedKeys = new ArrayList<>();
        for (Map.Entry<String, String> item : unprocessed.entrySet()) {
            String paramKey = item.getKey();
            String paramVal = item.getValue();
            if (!paramKey.startsWith("category_")) continue;

            if (hasCategory) {
                String cStrB32 = paramKey.replaceAll("category_", "");
                String cStr = new String(new Base32().decode(cStrB32), StandardCharsets.UTF_8);
                if (Boolean.parseBoolean(paramVal)) {
                    i.addCategory(cStr);
                }
            }
            proceedKeys.add(paramKey);
        }
        for (String key : proceedKeys) {
            unprocessed.remove(key);
        }
        proceedKeys.clear();

        // Parse extra
        if (hasExtra) {
            Map<String, Bundle> bundleMap = new HashMap<>();
            for (Map.Entry<String, String> item : unprocessed.entrySet()) {
                String paramKey = item.getKey();
                String paramVal = item.getValue();
                if (!paramKey.startsWith("extra_")) continue;

                Pattern pattern = Pattern.compile("^extra_(?<parentId>\\d+)_(?<nodeId>\\d+)_" +
                        "(?<nodeName>[A-Za-z\\d]+)_(?<nodeType>[A-Za-z\\d_$.]+)$");
                Matcher matcher = pattern.matcher(paramKey);
                if (!matcher.find() || !(matcher.groupCount() == 4))
                    throw new RuntimeException("Extra name pattern match failed: " + paramKey);
                String parentId = matcher.group(1);
                String id = matcher.group(2);
                String name = new String(new Base32().decode(matcher.group(3)), StandardCharsets.UTF_8);
                String type = new String(new Base32().decode(matcher.group(4)), StandardCharsets.UTF_8);

                Bundle parentBd;
                if (!bundleMap.containsKey(parentId)) {
                    parentBd = new Bundle();
                    bundleMap.put(parentId, parentBd);
                } else {
                    parentBd = bundleMap.get(parentId);
                }
                assert parentBd != null;

                if (Constants.VAL_NULL.equals(paramVal)) {
                    // null Object has no type, so use putBundle() here directly.
                    parentBd.putBundle(name, null);
                } else {
                    if (type.equals("Bundle")) {
                        Bundle bd;
                        if (!bundleMap.containsKey(id)) {
                            bd = new Bundle();
                            bundleMap.put(id, bd);
                        } else {
                            bd = bundleMap.get(id);
                        }
                        parentBd.putBundle(name, bd);
                    } else {
                        Object valObj = this.getFieldValue(paramVal, type);
                        // value is not VAL_NULL, but return is null
                        // means the type is unsupported.
                        if (valObj != null) {
                            try {
                                Method[] methods = Bundle.class.getMethods();
                                Method putMethod = null;
                                for (Method method : methods) {
                                    if (method.getName().equalsIgnoreCase("put" + type)) {
                                        putMethod = method;
                                        break;
                                    }
                                }
                                if (putMethod == null) {
                                    Log.e(TAG, "Bundle method [put" + type + "] not found!");
                                } else {
                                    putMethod.invoke(parentBd, name, valObj);
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Log.e(TAG, "Exception occurred when putting parameter into Bundle");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                proceedKeys.add(paramKey);
            }
            Bundle root = bundleMap.get("0");
            if (root != null) i.putExtras(root);
        } else {
            for (String key : unprocessed.keySet()) {
                if (key.startsWith("extra_")) proceedKeys.add(key);
            }
        }
        for (String key : proceedKeys) {
            unprocessed.remove(key);
        }

        if (unprocessed.size() > 0) {
            Log.w(TAG, "Unprocessed columns detected in testcase: " + unprocessed);
        }
        i.setClassName(this.pkgName, this.compName);
        return i;
    }
}
