package org.square16.ictdroid.testbridge.controller.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.testbridge.Constants;
import org.square16.ictdroid.testbridge.controller.TestController;
import org.square16.ictdroid.testbridge.controller.rpc.RPCClientHandler;
import org.square16.ictdroid.testbridge.controller.rpc.RPCHandler;
import org.square16.ictdroid.testbridge.utils.CSVTestCaseMgr;

import java.io.FileNotFoundException;

public class LoadHandler extends RPCHandler {
    public static final String TAG = "LoadHandler";

    public LoadHandler(TestController controller) {
        super(controller);
    }

    @Override
    public void handle(RPCClientHandler clientHandler, JSONObject recvObj) {
        assert recvObj.getInteger("action").equals(Constants.ACTION_LOAD);
        JSONObject resObj = new JSONObject();
        JSONObject recvDataObj = recvObj.getJSONObject("data");
        if (
                recvDataObj == null || !recvDataObj.containsKey("pkgName") ||
                        !recvDataObj.containsKey("compName") || !recvDataObj.containsKey("compType") ||
                        !recvDataObj.containsKey("strategy")
        ) {
            Log.e(TAG, "Invalid data received");
            resObj.put("code", Constants.CODE_ERROR_INVALID_REQ);
            clientHandler.sendResponse(resObj);
            return;
        }
        try {
            CSVTestCaseMgr testCaseMgr = new CSVTestCaseMgr(recvDataObj.getString("pkgName"),
                    recvDataObj.getString("compName"), recvDataObj.getString("strategy"));
            clientHandler.setContextVar("testCaseMgr", testCaseMgr);
            clientHandler.setContextVar("loadParams", recvDataObj);
            JSONObject resDataObj = new JSONObject();
            resDataObj.put("count", testCaseMgr.getSize());
            resObj.put("code", Constants.CODE_SUCCESS);
            resObj.put("data", resDataObj);
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            resObj.put("code", Constants.CODE_ERROR_LOAD_FILE_NOT_FOUND);
        }
        clientHandler.sendResponse(resObj);
    }
}
