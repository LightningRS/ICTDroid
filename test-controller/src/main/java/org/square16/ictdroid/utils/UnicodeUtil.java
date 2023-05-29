package org.square16.ictdroid.utils;

/**
 * @author Zsx
 */
public class UnicodeUtil {
    /**
     * 字符串编码成 Unicode
     */
    public static String encode(String src) throws Exception {
        char c;
        StringBuilder str = new StringBuilder();
        int intAsc;
        String strHex;
        for (int i = 0; i < src.length(); i++) {
            c = src.charAt(i);
            intAsc = (int) c;
            strHex = Integer.toHexString(intAsc);
            if (intAsc > 128) {
                str.append("\\u").append(strHex);
            } else {
                str.append("\\u00").append(strHex);
            }
        }
        return str.toString();
    }

    /**
     * Unicode 解码成字符串
     *
     * @param src Unicode 编码字符串
     * @return 解码后字符串
     */
    public static String decode(String src) {
        int t = src.length() / 6;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < t; i++) {
            String s = src.substring(i * 6, (i + 1) * 6);
            String s1 = s.substring(2, 4) + "00";
            String s2 = s.substring(4);
            int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
            char[] chars = Character.toChars(n);
            str.append(new String(chars));
        }
        return str.toString();
    }
}
