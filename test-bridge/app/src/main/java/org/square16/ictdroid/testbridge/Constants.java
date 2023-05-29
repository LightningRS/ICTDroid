package org.square16.ictdroid.testbridge;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {
    public static final int ERR_PERMISSION_DENIED = 1001;
    public static final int MSG_INIT_OK = 0;
    public static final int MSG_START_ACTIVITY = 1;

    public static final int CLIENT_VERSION = 1;

    public static final int ACTION_INIT = 1;
    public static final int ACTION_LOAD = 2;
    public static final int ACTION_RUN_CASE = 3;
    public static final int ACTION_RUN_CASE_RESULT = 4;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_ERROR_INVALID_SEQ = 1;
    public static final int CODE_ERROR_INVALID_REQ = 2;
    public static final int CODE_ERROR_INVALID_ACTION = 3;
    public static final int CODE_ERROR_NOT_LOAD = 4;
    public static final int CODE_ERROR_LOAD_FILE_NOT_FOUND = -1;
    public static final int CODE_ERROR_START_COMPONENT = -2;

    public static final String COMP_TYPE_ACTIVITY = "a";
    public static final String COMP_TYPE_SERVICE = "s";
    public static final String COMP_TYPE_RECEIVER = "r";

    public static final String VAL_NULL = "$#NULL#$";
    public static final String VAL_EMPTY = "$#EMPTY#$";
    public static final String VAL_NOT_EMPTY = "$#NOTEMPTY#$";

    public static final String VAL_NOT_EMPTY_ARR_NULL_ELEM = "$#NOTEMPTY_NULL#$";
    public static final String VAL_NOT_EMPTY_ARR_EMPTY_ELEM = "$#NOTEMPTY_EMPTY#$";
    public static final String VAL_NOT_EMPTY_ARR_NOT_EMPTY_ELEM = "$#NOTEMPTY_NOTEMPTY#$";

    public static final String DEFAULT_PARCELABLE_UUID = "11111111-1111-1111-1111-111111111111";
    public static final String DEFAULT_NOT_EMPTY_STR = "TestString";
    public static final String DEFAULT_SERIALIZABLE_STR = "TestSerializable";

    public static final ArrayList<String> EXTRA_SUPPORT_TYPES = new ArrayList<String>() {{
        addAll(Arrays.asList(
                "Boolean", "Byte", "Char", "Short", "Int", "Long", "Float", "Double",
                "String", "CharSequence", "IntegerArrayList", "StringArrayList",
                "CharSequenceArrayList", "Serializable", "BooleanArray",
                "ByteArray", "ShortArray", "CharArray", "IntArray", "LongArray",
                "FloatArray", "DoubleArray", "StringArray", "CharSequenceArray",
                "Parcelable", "ParcelableArrayList"
        ));
    }};
}
