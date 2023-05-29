package org.square16.ictdroid.rpc.handler;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.TestController;
import org.square16.ictdroid.rpc.CompStateMonitor;
import org.square16.ictdroid.rpc.RPCController;
import org.square16.ictdroid.rpc.annotations.RPCHandler;
import org.square16.ictdroid.rpc.interfaces.IRPCHandler;

@Slf4j
@RPCHandler(name = "RunCaseRecvHandler")
public class RunCaseRecvHandler implements IRPCHandler {
    @Override
    public void handle(RPCController controller, JSONObject dataObj) {
        TestController testController = controller.getTestController();
        CompStateMonitor compStateMonitor = testController.getCompStateMonitor();
        Integer code = dataObj.getInteger("code");
        if (code == null || code != 0) {
            if (Integer.valueOf(IRPCHandler.CODE_ERROR_LOAD_FILE_NOT_FOUND).equals(code)) {
                log.error("Remote testcase file not found!");
            } else if (Integer.valueOf(IRPCHandler.CODE_ERROR_NOT_LOAD).equals(code)) {
                controller.setLoaded(false);
                compStateMonitor.setCompState(CompStateMonitor.STATE_CLIENT_ERROR);
                log.error("Remote report not load! Reload testcase required");
                controller.reloadTestcase();
            } else {
                log.error("Failed to start component! result code={}", code);
            }
            compStateMonitor.setCompState(CompStateMonitor.STATE_INTENT_ERR);
        }
    }
}
