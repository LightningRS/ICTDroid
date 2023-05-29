package org.square16.ictdroid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {
    public static final String CLIENT_PKG_NAME = "com.test.apptestclient";
    public static final String CLIENT_ACT_NAME = CLIENT_PKG_NAME + ".activities.MainActivity";
    public static final String CLIENT_COMP_NAME = CLIENT_PKG_NAME + "/.activities.MainActivity";
    public static final String CLIENT_CASE_ROOT = "/sdcard/ICCBot/testcases";

    public static final String MIST_TYPE_MAY_IA = "mayIA";
    public static final String MIST_TYPE_MUST_IA = "mustIA";
    public static final String MIST_TYPE_MAY_EA = "mayEA";
    public static final String MIST_TYPE_MUST_EA = "mustEA";
    public static final String MIST_TYPE_NOT_EXPORTED = "notExported";

    public static final String VAL_NULL = "$#NULL#$";
    public static final String VAL_EMPTY = "$#EMPTY#$";
    public static final String VAL_NOT_EMPTY = "$#NOTEMPTY#$";

    public static final String VAL_NOT_EMPTY_ARR_NULL_ELEM = "$#NOTEMPTY_NULL#$";
    public static final String VAL_NOT_EMPTY_ARR_EMPTY_ELEM = "$#NOTEMPTY_EMPTY#$";
    public static final String VAL_NOT_EMPTY_ARR_NOT_EMPTY_ELEM = "$#NOTEMPTY_NOTEMPTY#$";

    public static final long TIMEOUT_START_MS = 5000;
    public static final long TIME_DISPLAY_REQUIRE_MS = 1000;
    public static final long COMP_CHECK_INTERVAL = 200;

    public static final int DEFAULT_RAND_VAL_NUM = 5;
    public static final int DEFAULT_RAND_STR_MIN_LENGTH = 1;
    public static final int DEFAULT_RAND_STR_MAX_LENGTH = 20;
    public static final int CASE_MAX_RETRY = 3;
    public static final int NUM_OF_PATH_THRESHOLD = 2;

    public static final String DEFAULT_SCOPE_CONFIG = """
            ,manifest,sendIntent,recvIntent
            action,0,1,1
            category,0,1,1
            data,0,1,1
            extra,0,1,1
            flag,0,1,1
            type,0,1,1
            """;

    public static final String[] PRESET_VALUES_CATEGORY = {
            // One invalid value
            "INVALID_CATEGORY",

            // Since Android system will automatically add Intent.CATEGORY_DEFAULT
            // ("android.intent.category.DEFAULT") to any implicit intent, it may be
            // no need to put this value into preset values.
            // "android.intent.category.DEFAULT",

            // Treat any component as a launcher
            "android.intent.category.LAUNCHER",

            // "android.intent.category.TEST",
            // "android.intent.category.UNIT_TEST",
            // "android.intent.category.SAMPLE_CODE"
    };

    /* Reference: https://stackoverflow.com/a/24134677 */
    public static final String[] PRESET_VALUES_TYPE = {
            // One invalid value + four common values
            "INVALID_TYPE", "image/jpeg", "video/mp4", "text/plain", "application/vnd.android.package-archive",
            // "image/jpeg", "image/png", "image/gif",
            // "text/plain", "text/html", "text/xml",
            // "audio/mpeg", "audio/aac", "audio/wav", "audio/ogg", "audio/midi",
            // "video/mp4", "video/x-msvideo", "video/x-ms-wmv",
            // "application/pdf", "application/vnd.android.package-archive"
    };

    public static final String[] PRESET_VALUES_SCHEME = {
            // One invalid value + four common values
            "INVALID_SCHEME", "https", "tel", "sms", "geo",
            // "http", "https", "mailto", "ftp", "tel", "sms", "geo"
    };

    public static final String[] PRESET_VALUES_AUTHORITY = {
            // One invalid value + four common values
            "INVALID_AUTHORITY", "localhost", "127.0.0.1", "12345678", "0,0",
    };

    public static final String[] PRESET_VALUES_PATH = {
            // One invalid value + four common values
            "INVALID_PATH", "/", "/path", "/path?a=1", "/path/path"
            // "/", "/data", "/storage/emulated/0", "/sdcard"
    };

    public static final Set<String> PRIMITIVE_TYPES = new HashSet<>() {{
        addAll(Arrays.asList("boolean", "byte", "char", "short", "int", "long", "float", "double"));
    }};
}
