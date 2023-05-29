package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.testcase.provider.BaseValueProvider;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 实验设计：
 * 利用四种不同的 BaseValueProvider (ICCBot, Preset, Random, RandomWithStruct) 按照以下组合进行测试生成
 * 每种组合用【组合测试】和【完备测试】各生成一次
 * 需不需要每种组合都带上 Preset？
 * a) Preset
 * b) Preset + ICCBot
 * c) Preset + Random
 * d) Preset + RandomWithStruct
 * <p>
 * 阶段情况：
 * a) 400 个小应用上的实验，但跑的时候异常调用栈收集模块的逻辑有 bug，只留下了调用栈，没有异常类型。重写了部分检测和监控逻辑，还需要再跑一次
 * <p>
 * b) 检测逻辑优化，目前划分的测试结果：
 * 生成 Intent 失败
 * Intent 被系统拦截
 * Activity 启动后停留时间过短（跳转到同一 Package 的另一个 Activity 除外 *）
 * Activity 崩溃
 * Activity 启动后出现 ANR（不一定能发现）
 * Activity 超时未检出（跳转到同一 Package 的另一个 Activity 除外 *）
 * <p>
 * c) 纯 Random 策略能稳定生成的字段只包括 action, category 和 type。
 * 目前加入了针对 data 的三个字段 path, scheme 和 authority 的分别生成，但效果并不理想
 * <p>
 * d) RandomWithStruct 由于考虑了 ICCBot 分析的 Component Model，实际生成的字段数量会低于纯 Random 策略
 * <p>
 * e) Category 是多值的，做进一步的扁平化处理，N 个值拆分为 N 个布尔变量，表示是否包括该 category。参数数量会显著增多，需要重新补充实验
 * <p>
 * f) 组合强度问题。现在设定的强度：
 * (3, action, category, data, extra, type)              基础字段之间 3-way
 * (min(size, 3), category1, category2, ...)             扁平化后的全部 category 之间 3-way
 * (min(size, 3), extra_0_1, extra_1_2, extra_2_3, ...)  扁平化后的全部 extra 之间 3-way
 * 目前，组合测试 Preset + ICCBot 策略下，单个组件测试用例数量 100~300 个。
 * 全部四种策略下（random r=5)，组合测试单个组件测试用例数量 900~4500 个。
 */
@Slf4j
public abstract class BaseTestcaseBuilder {
    private static final Config GlobalConfig = Config.getInstance();
    protected final CompModel compModel;
    protected final ScopeConfig scopeConfig;
    protected final JSONObject valueSet;
    protected boolean isValueSetModified = false;
    protected List<BaseValueProvider> valueProviders;

    public BaseTestcaseBuilder(CompModel compModel) {
        this(compModel, new ScopeConfig());
        boolean hasPath = GlobalConfig.getScopeConfigPath() != null;
        scopeConfig.loadScopeConfig(hasPath ? GlobalConfig.getScopeConfigPath().toString() : Constants.DEFAULT_SCOPE_CONFIG, hasPath);
    }

    public BaseTestcaseBuilder(CompModel compModel, ScopeConfig scopeConfig) {
        valueProviders = new ArrayList<>();
        this.compModel = compModel;
        this.scopeConfig = scopeConfig;
        valueSet = new JSONObject();
        buildBaseValueSet();
    }

    private void buildBaseValueSet() {
        JSONArray objBaseValues = new JSONArray();
        objBaseValues.addAll(Arrays.asList(Constants.VAL_NULL, Constants.VAL_EMPTY));
        // Deep copy
        valueSet.put("action", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        valueSet.put("category", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        valueSet.put("type", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        valueSet.put("data", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        valueSet.put("scheme", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        valueSet.put("authority", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        valueSet.put("path", JSONArray.parseArray(JSONArray.toJSONString(objBaseValues)));
        // extra will handle by ExtraFlattener
    }

    public boolean isValueSetModified() {
        return isValueSetModified;
    }

    public void addValueSet(JSONObject valueSet) {
        addValueSet(valueSet, false);
    }

    public void addValueSet(JSONObject valueSet, boolean isBasic) {
        if (valueSet == null || valueSet.size() == 0) {
            return;
        }
        for (String fieldName : valueSet.keySet()) {
            Object fieldValues = valueSet.get(fieldName);
            if (!(fieldValues instanceof JSONArray)) {
                log.error("Cannot merge type [{}] into value set", fieldValues.getClass().getName());
                continue;
            }
            if (Arrays.asList("scheme", "authority", "path").contains(fieldName) && ((JSONArray) fieldValues).size() > 0) {
                JSONArray dataArr = this.valueSet.getJSONArray("data");
                if (!dataArr.contains(Constants.VAL_NOT_EMPTY)) {
                    dataArr.add(Constants.VAL_NOT_EMPTY);
                }
            }
            JSONArray orgArr = this.valueSet.getJSONArray(fieldName);
            if (orgArr == null) {
                orgArr = new JSONArray();
                this.valueSet.put(fieldName, orgArr);
            }
            BaseValueProvider.mergeCompJSONArrRecur(orgArr, (JSONArray) fieldValues);
            if (!isBasic) {
                isValueSetModified = true;
            }
        }
    }

    public JSONObject getValueSet() {
        return new JSONObject(valueSet);
    }

    public ScopeConfig getScopeConfig() {
        return scopeConfig;
    }

    public void addValueProvider(BaseValueProvider valueProvider) {
        valueProviders.add(valueProvider);
    }

    public void collect() {
        for (BaseValueProvider provider : valueProviders) {
            JSONObject newValueSet = provider.getValueSet();
            addValueSet(newValueSet, "preset".equals(provider.getName()));
        }
    }

    public abstract void build(String outputCSVPath);
}
