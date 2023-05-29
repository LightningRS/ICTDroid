package org.square16.ictdroid.testcase;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ScopeConfig {
    protected final Map<String, Map<String, Boolean>> mFieldToScopeMap;

    public ScopeConfig() {
        mFieldToScopeMap = new HashMap<>();
    }

    public ScopeConfig(ScopeConfig config) {
        this();
        for (Map.Entry<String, Map<String, Boolean>> entry : config.mFieldToScopeMap.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Boolean> fieldMap = entry.getValue();
            for (Map.Entry<String, Boolean> fieldEntry : fieldMap.entrySet()) {
                this.setFieldScopeConfig(fieldName, fieldEntry.getKey(), fieldEntry.getValue());
            }
        }
    }

    public boolean getScopeConfig(String fieldName, String scopeName) {
        Map<String, Boolean> scopeMap = mFieldToScopeMap.get(fieldName);
        if (scopeMap == null) {
            return false;
        }
        return scopeMap.get(scopeName);
    }

    public void setFieldScopeConfig(String fieldName, String scopeName, boolean value) {
        Map<String, Boolean> scopeMap = mFieldToScopeMap.computeIfAbsent(fieldName, k -> new HashMap<>());
        scopeMap.put(scopeName, value);
    }

    public void replaceFieldConfig(String fieldName, Boolean value) {
        if (mFieldToScopeMap.containsKey(fieldName)) {
            for (Map.Entry<String, Boolean> entry : mFieldToScopeMap.get(fieldName).entrySet()) {
                entry.setValue(value);
            }
        } else {
            log.error("Invalid fieldName: {}", fieldName);
            throw new RuntimeException(String.format("Invalid fieldName: %s", fieldName));
        }
    }

    public void replaceScopeConfig(String scopeName, Boolean value) {
        for (Map.Entry<String, Map<String, Boolean>> entry : mFieldToScopeMap.entrySet()) {
            Map<String, Boolean> fieldMap = entry.getValue();
            if (fieldMap.containsKey(scopeName)) {
                fieldMap.put(scopeName, value);
            } else {
                log.error("Invalid scopeName: {}", scopeName);
                throw new RuntimeException(String.format("Invalid scopeName: %s", scopeName));
            }
        }
    }

    public void loadScopeConfig(String pathOrContent, boolean isPath) {
        try (CSVReader csvReader = new CSVReader(isPath ? new FileReader(pathOrContent) : new StringReader(pathOrContent))) {
            String[] row = csvReader.readNext();
            List<String> scopeNames = new ArrayList<>();
            for (String scopeName : row) {
                if (!"".equals(scopeName)) {
                    scopeNames.add(scopeName);
                }
            }
            while ((row = csvReader.readNext()) != null) {
                if (row.length > 0) {
                    String fieldName = row[0];
                    for (int i = 1; i < row.length && i - 1 < scopeNames.size(); i++) {
                        setFieldScopeConfig(fieldName, scopeNames.get(i - 1), "1".equals(row[i]));
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Failed to read scope config csv file [{}]", pathOrContent, e);
        }
    }
}
