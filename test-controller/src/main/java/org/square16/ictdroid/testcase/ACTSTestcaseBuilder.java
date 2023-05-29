package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.opencsv.CSVWriter;
import edu.uta.cse.fireeye.common.*;
import edu.uta.cse.fireeye.service.engine.IpoEngine;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.utils.CompModel;
import org.square16.ictdroid.utils.Config;
import org.square16.ictdroid.utils.MISTUtils;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ACTSTestcaseBuilder extends BaseTestcaseBuilder {
    private final SUT sut;

    public ACTSTestcaseBuilder(CompModel compModel) {
        this(compModel, new ScopeConfig());
    }

    public ACTSTestcaseBuilder(CompModel compModel, ScopeConfig scopeConfig) {
        super(compModel, scopeConfig);
        sut = new SUT(compModel.getClassName());
    }

    protected static String getParameterString(Parameter parameter) {
        StringBuilder res = new StringBuilder();
        String paramName = parameter.getName();
        if (paramName.startsWith("category_")) {
            paramName += " (" + BaseFlattener.b32decode(paramName.replace("category_", "")) + ")";
        } else if (paramName.startsWith("extra_")) {
            String[] sp = paramName.split("_");
            paramName += " (" + BaseFlattener.b32decode(sp[4]) + " " + BaseFlattener.b32decode(sp[3]) + ")";
        }
        res.append(paramName).append(": [");
        List<String> values = new ArrayList<>(parameter.getValues());
        values.sort((s1, s2) -> {
            if (Constants.VAL_NULL.equals(s1)) {
                return -1;
            }
            if (Constants.VAL_NULL.equals(s2)) {
                return 1;
            }
            if (Constants.VAL_EMPTY.equals(s1)) {
                return -1;
            }
            if (Constants.VAL_EMPTY.equals(s2)) {
                return 1;
            }
            return s2.compareTo(s1);
        });
        values.addAll(parameter.getInvalidValues());
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (!value.startsWith("$#") || !value.endsWith("#$")) {
                values.set(i, "\"" + value + "\"");
            }
        }
        res.append(String.join(", ", values));
        res.append("]\n");
        return res.toString();
    }

    @Override
    public void build(String outputCSVPath) {
        if (!isValueSetModified) {
            log.warn("No value collected, ignored generation");
            return;
        }
        // Flatten extra data
        new FlattenerExtra().flatten(valueSet);
        new FlattenerCategory().flatten(valueSet);

        // Add all parameters using sorted key set
        // head means that the parameter with the field name itself will be
        // placed at the head of the list.
        ArrayList<Parameter> catParams = new ArrayList<>();
        ArrayList<Parameter> extraParams = new ArrayList<>();
        for (String pName : new TreeSet<>(valueSet.keySet())) {
            Parameter param = sut.addParam(pName);
            if (pName.startsWith("category_")) {
                catParams.add(param);
            }
            if (pName.startsWith("extra_")) {
                extraParams.add(param);
            }
            JSONArray values = valueSet.getJSONArray(pName);
            for (String value : values.toJavaList(String.class)) {
                param.addValue(value);
            }
            param.setType(Parameter.PARAM_TYPE_ENUM);
        }

        // Build constraints for category
        if (catParams.size() > 0) {
            List<String> catConStrTrue = new ArrayList<>();
            List<String> catConStrFalse = new ArrayList<>();
            // Build condition statement
            catParams.forEach(c -> catConStrTrue.add(c.getName() + " = \"true\""));
            catParams.forEach(c -> catConStrFalse.add(c.getName() + " = \"false\""));
            // Append category itself
            ArrayList<Parameter> catParamsWithCat = new ArrayList<>(catParams);
            catParamsWithCat.add(sut.getParam("category"));
            // Build forward constraint
            Constraint catConForward = new Constraint("category = \"" + Constants.VAL_EMPTY + "\" => " +
                    String.join(" && ", catConStrFalse), catParamsWithCat);
            sut.addConstraint(catConForward);
            // Build reverse constraint
            Constraint catConReverse = new Constraint(String.join(" || ", catConStrTrue) + " => " +
                    "category = \"" + Constants.VAL_NOT_EMPTY + "\"", catParamsWithCat);
            sut.addConstraint(catConReverse);
        }

        // Build constraints for extra
        if (extraParams.size() > 0) {
            List<String> extraConStrNull = new ArrayList<>();
            // List<String> extraConStrNotNull = new ArrayList<>();
            // Build condition statement
            extraParams.forEach(c -> extraConStrNull.add(c.getName() + " = \"" + c.getValue(0) + "\""));
            // extraParams.forEach(c -> extraConStrNotNull.add(c.getName() + " == \"" + Constants.VAL_NULL + "\""));
            // Append category itself
            ArrayList<Parameter> extraParamsWithExtra = new ArrayList<>(extraParams);
            extraParamsWithExtra.add(sut.getParam("extra"));
            // Build forward constraint
            Constraint extraConForward = new Constraint("extra = \"" + Constants.VAL_NULL
                    + "\" || extra = \"" + Constants.VAL_EMPTY
                    + "\" => " + String.join(" && ", extraConStrNull), extraParamsWithExtra);
            sut.addConstraint(extraConForward);
            // Build reverse constraint
            // Constraint extraConReverse = new Constraint(String.join(" || ", extraConStrNotNull) + " => "
            //         + "extra = \"" + Constants.VAL_NOT_EMPTY + "\"", extraParamsWithExtra);
            // sut.addConstraint(extraConReverse);

            // Build constraints for bundles
            for (Parameter extraParam : extraParams) {
                if (!extraParam.getName().endsWith("_bundle")) {
                    continue;
                }
                String bdName = extraParam.getName();
                Pattern pattern = Pattern.compile("^extra_(?<parentId>\\d+)_(?<nodeId>\\d+)_" +
                        "(?<nodeName>[A-Za-z\\d]+)_(?<nodeType>[A-Za-z\\d_$.]+)$");
                Matcher matcher = pattern.matcher(bdName);
                if (!matcher.find() || matcher.groupCount() != 4) {
                    throw new RuntimeException("Extra name pattern match failed: " + bdName);
                }
                String nodeId = matcher.group("nodeId");
                ArrayList<Parameter> childParams = new ArrayList<>();
                List<String> childParamsConNull = new ArrayList<>();
                List<String> childParamsConNotNull = new ArrayList<>();
                for (Parameter param : extraParams) {
                    if (param.getName().startsWith("extra_" + nodeId + "_")) {
                        childParams.add(param);
                        childParamsConNull.add(param.getName() + " = \"" + Constants.VAL_NULL + "\"");
                        childParamsConNotNull.add(param.getName() + " != \"" + Constants.VAL_NULL + "\"");
                    }
                }
                if (childParams.size() == 0) {
                    continue;
                }

                // child parameters relation
                // if (childParams.size() > 1) {
                //     Relation rChildParams = new Relation(Math.min(childParams.size(), 3));
                //     childParams.forEach(rChildParams::addParam);
                //     sut.addRelation(rChildParams);
                // }

                childParams.add(extraParam);
                Constraint fCon = new Constraint(bdName + " = \"" + Constants.VAL_NULL + "\" || " +
                        bdName + " = \"" + Constants.VAL_EMPTY + "\" => " +
                        String.join(" && ", childParamsConNull), childParams
                );
                Constraint rCon = new Constraint(String.join(" || ", childParamsConNotNull) + " => " +
                        bdName + " = \"" + Constants.VAL_NOT_EMPTY + "\"", childParams
                );
                sut.addConstraint(fCon);
                sut.addConstraint(rCon);
            }
        }

        // Build constraints for data
        ArrayList<Parameter> dataParams = new ArrayList<>(Arrays.asList(
                sut.getParam("data"),
                sut.getParam("scheme"),
                sut.getParam("authority"),
                sut.getParam("path")
        ));
        Constraint dataConForward = new Constraint("data = \"" + Constants.VAL_NULL + "\" || data = \"" +
                Constants.VAL_EMPTY + "\" => " + "scheme = \"" + Constants.VAL_EMPTY + "\" && authority = \"" +
                Constants.VAL_EMPTY + "\" && path = \"" + Constants.VAL_EMPTY + "\"", dataParams);
        sut.addConstraint(dataConForward);

        Constraint dataConForward2 = new Constraint("data = \"" + Constants.VAL_NOT_EMPTY + "\" => ( " +
                "scheme != \"" + Constants.VAL_NULL + "\" && scheme != \"" + Constants.VAL_EMPTY + "\" ) || ( " +
                "authority != \"" + Constants.VAL_NULL + "\" && authority != \"" + Constants.VAL_EMPTY + "\" ) || ( " +
                "path != \"" + Constants.VAL_NULL + "\" && path != \"" + Constants.VAL_EMPTY + "\" )", dataParams);
        sut.addConstraint(dataConForward2);

//        Constraint dataConReverse1 = new Constraint("scheme != \"" + Constants.VAL_EMPTY + "\" && " +
//                "(authority != \"" + Constants.VAL_EMPTY + "\" || path != \"" + Constants.VAL_EMPTY + "\") => " +
//                "data = \"" + Constants.VAL_NOT_EMPTY + "\"", dataParams);
//        Constraint dataConReverse2 = new Constraint(
//                "scheme = \"" + Constants.VAL_EMPTY + "\" => data = \"" + Constants.VAL_EMPTY + "\"",
//                new ArrayList<>(Arrays.asList(sut.getParam("data"), sut.getParam("scheme")))
//        );
//        sut.addConstraint(dataConReverse1);
//        sut.addConstraint(dataConReverse2);

        // Default strength
        int defaultS = Config.getInstance().getDefaultStrength();

        // Relation between basic fields
        Relation rAllFields = new Relation();
        if (defaultS != 0) {
            rAllFields.setStrength(defaultS);
        } else {
            rAllFields.setStrength(1);
        }
        rAllFields.addParam(sut.getParam("action"));
        rAllFields.addParam(sut.getParam("category"));
        rAllFields.addParam(sut.getParam("data"));
        rAllFields.addParam(sut.getParam("extra"));
        rAllFields.addParam(sut.getParam("type"));
        addRelation(rAllFields);

        int uniqueS = 3;
        // Strategy for default relation:
        // if useAction and useExtra => t = 3
        // else if numOfPath > threshold (2) => t = 3
        // else t = 1 (mustIA, numOfPath = 0, etc.)
        if (defaultS == 0) {
            boolean hasMISTResult = Config.getInstance().getMISTResult() != null;
            boolean hasParamSummary = getCompParamSummaryJson() != null;
            boolean isUseActAndExt = isUseActionAndExtra();
            int numOfPath = getNumOfPath();
            if (isUseActAndExt) {
                log.info("Default Relation: Use action and extra");
            } else if (numOfPath > Constants.NUM_OF_PATH_THRESHOLD) {
                log.info("Default Relation: numOfPath > THRESHOLD");
            } else {
                if (hasMISTResult && Constants.MIST_TYPE_MUST_IA.equals(compModel.getMistType())) {
                    log.info("Default Relation: MIST result is mustIA");
                }
                if (hasParamSummary && numOfPath == 0) {
                    log.info("Default Relation: numOfPath = 0");
                }
                uniqueS = 1;
            }
        }

        // Relation between flattened categories
        if (catParams.size() > 1) {
            Relation rCategory = new Relation();
            if (defaultS > 0) {
                rCategory.setStrength(defaultS);
            } else if (compModel.hasFieldScopeValues("sendIntent", "category")
                    || compModel.hasFieldScopeValues("recvIntent", "category")) {
                log.debug("Category has been used in send/recv scope, set strength to uniqueS");
                rCategory.setStrength(uniqueS);
            } else {
                rCategory.setStrength(1);
            }
            catParams.forEach(rCategory::addParam);
            addRelation(rCategory);
        }

        // Relation between extra
        // Dynamically set by updateRelationByParamSummary, ignored
//        if (extraParams.size() > 1) {
//            Relation rExtra = new Relation();
//            rExtra.setStrength(defaultS > 0 ? defaultS : 2);
//            extraParams.forEach(rExtra::addParam);
//            addRelation(rExtra);
//        }

        // Relation between data
        Relation rData = new Relation();
        rData.addParam(sut.getParam("scheme"));
        rData.addParam(sut.getParam("authority"));
        rData.addParam(sut.getParam("path"));
        if (defaultS > 0) {
            rData.setStrength(defaultS);
        } else if (MISTUtils.isDataUsed(compModel)) {
            log.debug("Data has been used in send/recv scope, set strength to uniqueS");
            rData.setStrength(uniqueS);
        } else {
            rData.setStrength(1);
        }
        addRelation(rData);

        if (defaultS == 0) {
            // Optimize strength by param summary
            updateRelationByParamSummary(uniqueS);
        } else {
            // Optimize strength by param summary
            updateRelationByParamSummary(defaultS);
        }

        // Default Relation
        if (defaultS == 0) {
            sut.addDefaultRelation(1);
        } else {
            log.info("Default Relation: Fixed {}", defaultS);
            sut.addDefaultRelation(defaultS);
        }

        log.info("Generated SUT:\n{}", getSUTString());

        // Generate
        TestGenProfile profile = TestGenProfile.instance();
        profile.setIgnoreConstraints(false);
        profile.setConstraintMode(TestGenProfile.ConstraintMode.forbiddentuples);
        IpoEngine engine = new IpoEngine(sut);
        engine.build();
        TestSet ts = engine.getTestSet();
        // TestSetWrapper wrapper = new TestSetWrapper(ts, sut);
        // wrapper.outputInCSVFormat(outputCSVPath);

        // To CSV
        File file = new File(outputCSVPath);
        try {
            PrintWriter writer = new PrintWriter(file);
            int numOfTests = ts.getNumOfTests();
            int numOfParams = ts.getNumOfParams();
            CSVWriter csvWriter = new CSVWriter(writer);

            // Count full-combination size
            List<Integer> domainSizes = new ArrayList<>();
            for (int i = 0; i < numOfParams; i++) {
                domainSizes.add(sut.getParam(i).getDomainSize());
            }
            AtomicReference<BigInteger> fullCombSize = new AtomicReference<>(new BigInteger("1"));
            domainSizes.forEach(d -> fullCombSize.set(fullCombSize.get().multiply(new BigInteger(String.valueOf(d)))));

            log.info("numOfParams = " + numOfParams + ", numOfTest = " + numOfTests);
            log.info("fullCombSize = " + fullCombSize.get());
            List<String[]> comments = new ArrayList<>();
            comments.add(new String[]{"ACTS Test Suite Generation: " + new Date()});
            comments.add(new String[]{" '*' represents don't care value "});
            comments.add(new String[]{"Degree of interaction coverage: " + TestGenProfile.instance().getDOI()});
            comments.add(new String[]{"Number of parameters: " + ts.getNumOfParams()});
            comments.add(new String[]{"Maximum number of values per parameter: " + sut.getMaxDomainSize()});
            comments.add(new String[]{"Number of configurations: " + ts.getNumOfTests()});
            csvWriter.writeAll(comments);

            List<String> paramNames = sut.getParams().stream().map(Parameter::getName).toList();
            csvWriter.writeNext(paramNames.toArray(new String[0]));

            for (int i = 0; i < numOfTests; ++i) {
                List<String> values = new ArrayList<>();
                for (int j = 0; j < numOfParams; ++j) {
                    Parameter param = sut.getParam(j);
                    int col = ts.getColumnID(param.getID());
                    int value = ts.getValue(i, col);
                    if (value <= -10) {
                        int invalidValueIndex = -1 * (value + 10);
                        values.add(param.getInvalidValue(invalidValueIndex));
                    } else if (value != -1) {
                        if (param.getParamType() == 1) {
                            values.add(param.getValue(value));
                        } else {
                            values.add(param.getValue(value));
                        }
                    } else {
                        values.add("*");
                    }
                }
                csvWriter.writeNext(values.toArray(new String[0]));
            }
            csvWriter.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getSUTString() {
        StringBuilder res = new StringBuilder();
        res.append("System Under Test:\n");
        res.append("===========================================\n");
        res.append("Name: ").append(sut.getName()).append("\n");
        res.append("Number of Params: ").append(sut.getNumOfParams()).append("\n");
        res.append("\nParameters: \n");
        for (Parameter parameter : sut.getParameters()) {
            res.append(getParameterString(parameter));
        }
        res.append("\nRelations: \n");
        for (Relation relation : sut.getRelations()) {
            res.append(relation);
        }
        res.append("\nConstraints: \n");

        for (Constraint constraint : sut.getConstraintManager().getConstraints()) {
            res.append(constraint);
        }
        return res.toString();
    }

    private JSONArray getCompParamSummaryJson() {
        JSONObject paramSummary = compModel.getAppModel().getParamSummaryJson();
        if (paramSummary == null) {
            return null;
        }
        // 获取当前组件的 paramSummary 列表
        return paramSummary.getJSONArray(compModel.getClassName());
    }

    private int getNumOfPath() {
        JSONArray compParamSummary = getCompParamSummaryJson();
        if (compParamSummary == null) {
            return 0;
        }
        return compParamSummary.size();
    }

    private boolean isUseActionAndExtra() {
        JSONArray compParamSummary = getCompParamSummaryJson();
        if (compParamSummary == null) {
            return false;
        }
        for (Object pathSummaryObj : compParamSummary) {
            boolean hasAction = false;
            boolean hasExtra = false;
            // 获取当前路径下的所有参数名称
            List<String> pathParams = ((JSONObject) pathSummaryObj).getJSONArray("params").toJavaList(String.class);
            for (String param : pathParams) {
                if ("action".equals(param)) {
                    hasAction = true;
                } else if (param.contains("-")) {
                    hasExtra = true;
                }
            }
            if (hasAction && hasExtra) {
                return true;
            }
        }
        return false;
    }

    private void updateRelationByParamSummary(int defaultS) {
        // 获取当前组件的 paramSummary 列表
        JSONArray compParamSummary = getCompParamSummaryJson();
        if (compParamSummary == null) {
            return;
        }

        // 构建一个参数组合列表，每个元素是一个参数组合
        // 元素用 Map 表示，其中 key 为参数在 paramSummary 中的名称
        // value 为参数在组合测试模型 (SUT) 中对应的 Parameter 对象
        List<Map<String, Parameter>> paramGroups = new ArrayList<>();

        for (Object pathSummaryObj : compParamSummary) {
            // 获取当前路径下的所有参数名称
            JSONArray pathParams = ((JSONObject) pathSummaryObj).getJSONArray("params");

            // 若没有参数或参数个数少于 2 个则忽略
            if (pathParams == null || pathParams.size() < 2) {
                continue;
            }

            Map<String, Parameter> paramMap = new HashMap<>(pathParams.size());
            AtomicBoolean hasExtra = new AtomicBoolean(false);

            // 将 JSONArray 转换为 List<String>，遍历参数列表
            pathParams.toJavaList(String.class).stream().filter(Predicate.not(String::isEmpty))
                    .forEach(val -> {

                        // 去除 Bundle 的左右括号值
                        if ("(".equals(val) || ")".equals(val)) {
                            return;
                        }

                        // 对于 action, category 等基本字段
                        // targetName 与 paramSummary 中的名称相同
                        String targetName = val;

                        // 对于 extra 字段，判断标准为是否包含减号“-”
                        // 需要进行名称转换 (base32 编码)
                        if (val.contains("-")) {
                            hasExtra.set(true);
                            String[] sp = val.split("-", 2);
                            String name = sp[1].trim();

                            // 将 name 名称部分进行 base32 编码 并加上前后下划线
                            // extra 字段在组合测试模型中的名称格式为 extra_{parentId}_{id}_{nameInBase32}_{typeInBase32}
                            // 因此只需判断是否包含 “_{nameInBase32}_” 即可找到 extra 字段在组合测试模型 (SUT) 中对应的 Parameter
                            targetName = "_" + FlattenerExtra.encodeExtraName(name) + "_";
                        }

                        // 在组合测试模型 (SUT) 中查找对应的 Parameter
                        Parameter target = null;
                        for (Parameter param : sut.getParams()) {

                            // 优先完全匹配，若不通过，则判断 targetName 是否以下划线开头
                            // 以下划线开头说明该字段是 extra 字段，按照 contains 方法进行匹配
                            if (param.getName().equals(targetName)
                                    || (targetName.startsWith("_") && param.getName().contains(targetName))
                            ) {
                                target = param;
                                break;
                            }
                        }

                        if (target == null) {
                            // 对应参数未找到，报错抛异常
                            String msg = String.format("Param [%s] (%s) not found in SUT! Cannot add relation!", val, targetName);
                            log.error(msg);
                            throw new RuntimeException(msg);
                        } else {
                            // 对应参数已找到，加入 paramMap
                            paramMap.put(val, target);
                        }
                    });
            if (hasExtra.get()) {
                log.info("Extra detected in param summary");
                paramMap.put("extra", sut.getParam("extra"));
            }
            paramGroups.add(paramMap);
        }

        // 按照 paramMap 的大小对 paramGroups 排序
        // 排序后，参数数量少的组合靠前，参数数量多的组合靠后
        paramGroups.sort(Comparator.comparingInt(Map::size));

        // removedIdx 集合用于标记【已被其它参数组合包含】的参数组合
        Set<Integer> removedIdx = new HashSet<>();
        for (int i = 0; i < paramGroups.size() - 1; i++) {
            for (int j = paramGroups.size() - 1; j > i; j--) {
                // 从参数数量最少的参数组合 (p[i]) 开始检查，优先与参数数量尽可能多的参数组合 (p[j]) 对比

                // 先假定 p[i] 已包含于 p[j]，flag = true
                // 遍历 p[i] 的所有参数，若 p[i] 中任一名称的参数 不属于 p[j]，则 p[i] 未包含于 p[j]，flag = false
                // 遍历结束后，若 flag 仍为 true，证明 p[i] 包含于 p[j]，将 i 加入 removedIdx 集合
                Set<String> groupShortKeySet = paramGroups.get(i).keySet();
                Set<String> groupLongKeySet = paramGroups.get(j).keySet();
                boolean flag = true;
                for (String shortKey : groupShortKeySet) {
                    if (!groupLongKeySet.contains(shortKey)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    removedIdx.add(i);
                    break;
                }
            }
        }
        for (int i = 0; i < paramGroups.size(); i++) {
            // 若 i 属于集合 removedIdx
            // 则证明 参数组合 p[i] 已包含于其它参数组合
            // 不再建立 Relation

            // 否则建立一个 3 强度的 Relation，加入 SUT
            if (!removedIdx.contains(i)) {
                Map<String, Parameter> paramGroup = paramGroups.get(i);
                log.info("Add relation by param summary: ({})",
                        String.join(", ", paramGroup.keySet().stream().sorted().toList()));
                Relation r = new Relation();
                r.setStrength(defaultS);
                paramGroup.values().forEach(r::addParam);
                addRelation(r);
            }
        }
    }

    private void addRelation(Relation r) {
        ArrayList<Parameter> rNewParams = r.getParams();
        ArrayList<Relation> relations = sut.getRelations();
        int existIdx = -1;
        for (int i = 0; i < relations.size(); i++) {
            Relation rOld = relations.get(i);
            ArrayList<Parameter> rOldParams = rOld.getParams();
            boolean existFlag = true;
            for (Parameter p : rNewParams) {
                if (!rOldParams.contains(p)) {
                    existFlag = false;
                    break;
                }
            }
            if (existFlag && rOld.getStrength() < r.getStrength()) {
                rOld.setStrength(r.getStrength());
                existIdx = i;
                break;
            }
        }
        if (existIdx == -1) {
            sut.addRelation(r);
        }
    }
}
