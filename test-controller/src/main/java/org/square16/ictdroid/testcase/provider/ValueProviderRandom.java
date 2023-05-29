package org.square16.ictdroid.testcase.provider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.text.RandomStringGenerator;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;
import org.square16.ictdroid.utils.GlobalRandom;

import java.util.Random;

public class ValueProviderRandom extends BaseValueProvider {
    private final int cntPerField;
    private final int strMinLength;
    private final int strMaxLength;
    protected RandomStringGenerator generator1;
    protected RandomStringGenerator generator2;
    protected RandomStringGenerator generatorUriAuthority;
    protected RandomStringGenerator generatorUriPath;
    protected Random random;

    public ValueProviderRandom(CompModel compModel) {
        this(compModel, 5);
    }

    public ValueProviderRandom(CompModel compModel, int cntPerField) {
        this(compModel, cntPerField, Constants.DEFAULT_RAND_STR_MIN_LENGTH, Constants.DEFAULT_RAND_STR_MAX_LENGTH);
    }

    public ValueProviderRandom(CompModel compModel, int cntPerField, int strMinLength, int strMaxLength) {
        super();
        this.name = "random";
        this.compModel = compModel;
        this.cntPerField = cntPerField;
        this.strMinLength = strMinLength;
        this.strMaxLength = strMaxLength;
    }

    private void resetRandom() {
        this.random = GlobalRandom.getInstance();
        this.generator1 = new RandomStringGenerator.Builder()
                .usingRandom(random::nextInt)
                .selectFrom("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._$".toCharArray())
                .build();
        this.generator2 = new RandomStringGenerator.Builder()
                .usingRandom(random::nextInt)
                .selectFrom("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray())
                .build();
        this.generatorUriAuthority = new RandomStringGenerator.Builder()
                .usingRandom(random::nextInt)
                .selectFrom("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:?#[]@!$&'()*+,;%="
                        .toCharArray())
                .build();
        this.generatorUriPath = new RandomStringGenerator.Builder()
                .usingRandom(random::nextInt)
                .selectFrom("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;%="
                        .toCharArray())
                .build();
    }

    @Override
    public JSONObject getValueSet() {
        if (!Config.getInstance().getWithRandom()) {
            return null;
        }
        resetRandom();
        JSONObject res = new JSONObject();
        int i;
        JSONArray actionArr = new JSONArray();
        for (i = 0; i < cntPerField; i++) {
            actionArr.add(generator1.generate(strMinLength, strMaxLength));
        }
        res.put("action", actionArr);

        JSONArray categoryArr = new JSONArray();
        for (i = 0; i < cntPerField; i++) {
            categoryArr.add(generator1.generate(strMinLength, strMaxLength));
        }
        res.put("category", categoryArr);

        JSONArray typeArr = new JSONArray();
        for (i = 0; i < cntPerField; i++) {
            typeArr.add(generator1.generate(strMinLength, strMaxLength));
        }
        res.put("type", typeArr);

        JSONArray schemeArr = new JSONArray();
        for (i = 0; i < cntPerField; i++) {
            schemeArr.add(generator2.generate(strMinLength, strMaxLength));
        }
        res.put("scheme", schemeArr);

        JSONArray authorityArr = new JSONArray();
        for (i = 0; i < cntPerField; i++) {
            authorityArr.add(generatorUriAuthority.generate(strMinLength, strMaxLength));
        }
        res.put("authority", authorityArr);

        JSONArray pathArr = new JSONArray();
        for (i = 0; i < cntPerField; i++) {
            pathArr.add(generatorUriPath.generate(strMinLength, strMaxLength));
        }
        res.put("path", pathArr);
        return res;
    }
}
