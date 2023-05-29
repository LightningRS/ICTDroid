package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base32;

import java.nio.charset.StandardCharsets;

public abstract class BaseFlattener {
    private static final Base32 BASE32 = new Base32();

    protected static String b32encode(String str) {
        return BASE32.encodeToString(str.getBytes(StandardCharsets.UTF_8)).replaceAll("=", "");
    }

    protected static String b32decode(String str) {
        return new String(BASE32.decode(str), StandardCharsets.UTF_8);
    }

    public abstract int flatten(JSONObject jsonObj);
}
