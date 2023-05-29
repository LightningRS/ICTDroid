package org.square16.ictdroid.utils;

public class ICCBotUtils {
    public static String getRealFieldName(String modelFieldName) {
        return switch (modelFieldName) {
            case "actions" -> "action";
            case "categories" -> "category";
            case "datas" -> "data";
            case "schemes" -> "scheme";
            case "hosts" -> "host";
            case "ports" -> "port";
            case "paths" -> "path";
            case "extras" -> "extra";
            case "flags" -> "flag";
            case "types" -> "type";
            default -> "unknown";
        };
    }

    public static String getModelFieldName(String fieldName) {
        return switch (fieldName) {
            case "action" -> "actions";
            case "category" -> "categories";
            case "data" -> "datas";
            case "scheme" -> "schemes";
            case "host" -> "hosts";
            case "port" -> "ports";
            case "path" -> "paths";
            case "extra" -> "extras";
            case "flag" -> "flags";
            case "type" -> "types";
            default -> "unknown";
        };
    }
}
