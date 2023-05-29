package org.square16.ictdroid.testbridge.controller.handlers;

import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.testbridge.Constants;
import org.square16.ictdroid.testbridge.controller.TestController;
import org.square16.ictdroid.testbridge.controller.rpc.RPCClientHandler;
import org.square16.ictdroid.testbridge.controller.rpc.RPCHandler;

public class InitHandler extends RPCHandler {
    public InitHandler(TestController controller) {
        super(controller);
    }

    @Override
    public void handle(RPCClientHandler clientHandler, JSONObject recvObj) {
        assert recvObj.getInteger("action").equals(Constants.ACTION_INIT);
        JSONObject dataObj = new JSONObject();
        dataObj.put("version", Constants.CLIENT_VERSION);
        JSONObject resObj = new JSONObject();
        resObj.put("code", Constants.CODE_SUCCESS);
        resObj.put("data", dataObj);
        clientHandler.sendResponse(resObj);
    }
}
