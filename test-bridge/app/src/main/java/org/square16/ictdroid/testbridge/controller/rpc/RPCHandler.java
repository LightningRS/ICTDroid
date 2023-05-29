package org.square16.ictdroid.testbridge.controller.rpc;

import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.testbridge.controller.TestController;

public abstract class RPCHandler {
    protected TestController mController;

    public RPCHandler(TestController controller) {
        this.mController = controller;
    }

    public abstract void handle(RPCClientHandler clientHandler, JSONObject recvObj);
}
