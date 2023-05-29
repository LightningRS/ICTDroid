package org.square16.ictdroid.rpc.handler;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.rpc.RPCController;
import org.square16.ictdroid.rpc.annotations.RPCHandler;
import org.square16.ictdroid.rpc.interfaces.IRPCHandler;

@Slf4j
@RPCHandler(name = "InitRecvHandler")
public class InitRecvHandler implements IRPCHandler {
    @Override
    public void handle(RPCController controller, JSONObject dataObj) {
        // TODO: Verify rpc version
        if (!dataObj.getInteger("code").equals(IRPCHandler.CODE_SUCCESS)) {
            log.error("Error when initializing rpc! received msg: {}", dataObj.toJSONString());
            return;
        }
        log.info("RPC Initialize OK");
        controller.setReady(true);
    }
}
