package org.square16.ictdroid.testcase;

import org.apache.commons.text.RandomStringGenerator;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.utils.GlobalRandom;

import java.util.Random;

public class RandomExtraDataGenerator {
    public static String generate(String extraType) {
        return generate(extraType, Constants.DEFAULT_RAND_STR_MIN_LENGTH, Constants.DEFAULT_RAND_STR_MAX_LENGTH);
    }

    public static String generate(String extraType, int strMinLength, int strMaxLength) {
        return generate(extraType, strMinLength, strMaxLength, GlobalRandom.getInstance());
    }

    public static String generate(String extraType, int strMinLength, int strMaxLength, Random random) {
        extraType = extraType.replaceAll("java\\.lang\\.", "");
        return switch (extraType) {
            case "byte", "Byte" -> String.valueOf(random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE));
            case "short", "Short" -> String.valueOf(random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE));
            case "int", "Integer" -> String.valueOf(random.nextInt());
            case "long", "Long" -> String.valueOf(random.nextLong());
            case "float", "Float" -> String.valueOf(random.nextFloat());
            case "double", "Double" -> String.valueOf(random.nextDouble());
            case "char", "Character" ->
                    String.valueOf(Character.toChars(random.nextInt(Character.MIN_VALUE, Character.MAX_VALUE)));
            case "String", "CharSequence", "char[]" -> new RandomStringGenerator.Builder().usingRandom(random::nextInt)
                    .selectFrom("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;%="
                            .toCharArray())
                    .build().generate(strMinLength, strMaxLength);
            default -> null;
        };
    }
}
