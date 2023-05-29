package org.square16.ictdroid.rpc.handler;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.TestController;
import org.square16.ictdroid.rpc.RPCController;
import org.square16.ictdroid.rpc.annotations.RPCHandler;
import org.square16.ictdroid.rpc.interfaces.IRPCHandler;

@Slf4j
@RPCHandler(name = "LoadRecvHandler")
public class LoadRecvHandler implements IRPCHandler {
    @Override
    public void handle(RPCController controller, JSONObject dataObj) {
        int code = dataObj.getIntValue("code");
        if (code != IRPCHandler.CODE_SUCCESS) {
            log.error("RPC returned error for ACTION_LOAD: {}", dataObj.toJSONString());
            controller.getTestController().setCurrCompState(TestController.STATE_ERROR);
            return;
        }
        int cnt = dataObj.getJSONObject("data").getIntValue("count");
        log.info("RPC loaded {} testcases", cnt);
        controller.getTestController().setCurrCaseCount(cnt);
        controller.getTestController().setCurrCompState(TestController.STATE_LOADED_TESTCASE);
        controller.setLoaded(true);
    }
}
