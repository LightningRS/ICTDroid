package org.square16.ictdroid.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MISTUtils {
    /**
     * Get component MIST type name.
     *
     * @param compModel Component model
     */
    public static void checkCompMISTType(CompModel compModel) {
        if (compModel == null) {
            return;
        }
        JSONObject mistResult = Config.getInstance().getMISTResult();
        if (mistResult == null) {
            // MIST result is not specified, ignore
            return;
        }
        if (!mistResult.containsKey(compModel.getPackageName())) {
            return;
        }
        JSONObject pkgResult = mistResult.getJSONObject(compModel.getPackageName());
        if (!pkgResult.containsKey(compModel.getClassName())) {
            log.debug("Component [{}/{}] is not in MIST result", compModel.getPackageName(), compModel.getClassName());
            return;
        }
        compModel.setMistType(pkgResult.getString(compModel.getClassName()));
    }

    public static boolean isDataUsed(CompModel compModel) {
        return compModel.hasFieldScopeValues("sendIntent", "data", "scheme")
                || compModel.hasFieldScopeValues("recvIntent", "data", "scheme")
                || compModel.hasFieldScopeValues("sendIntent", "data", "host")
                || compModel.hasFieldScopeValues("recvIntent", "data", "host")
                || compModel.hasFieldScopeValues("sendIntent", "data", "port")
                || compModel.hasFieldScopeValues("recvIntent", "data", "port")
                || compModel.hasFieldScopeValues("sendIntent", "data", "authority")
                || compModel.hasFieldScopeValues("recvIntent", "data", "authority")
                || compModel.hasFieldScopeValues("sendIntent", "data", "path")
                || compModel.hasFieldScopeValues("recvIntent", "data", "path");
    }
}
