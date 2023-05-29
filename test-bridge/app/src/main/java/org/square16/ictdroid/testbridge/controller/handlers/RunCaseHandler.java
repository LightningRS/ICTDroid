package org.square16.ictdroid.testbridge.controller.handlers;

import android.content.Intent;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.testbridge.Constants;
import org.square16.ictdroid.testbridge.controller.TestController;
import org.square16.ictdroid.testbridge.controller.rpc.RPCClientHandler;
import org.square16.ictdroid.testbridge.controller.rpc.RPCHandler;
import org.square16.ictdroid.testbridge.utils.CSVTestCaseMgr;

public class RunCaseHandler extends RPCHandler {
    public static final String TAG = "RunCaseHandler";

    public RunCaseHandler(TestController controller) {
        super(controller);
    }

    @Override
    public void handle(RPCClientHandler clientHandler, JSONObject recvObj) {
        assert recvObj.getInteger("action").equals(Constants.ACTION_RUN_CASE);
        JSONObject resObj = new JSONObject();
        JSONObject recvDataObj = recvObj.getJSONObject("data");
        if (recvDataObj == null || !recvDataObj.containsKey("caseId")) {
            Log.e(TAG, "Invalid data received");
            resObj.put("code", Constants.CODE_ERROR_INVALID_REQ);
            clientHandler.sendResponse(resObj);
            return;
        }
        Integer caseId = recvDataObj.getInteger("caseId");
        try {
            CSVTestCaseMgr testCaseMgr = (CSVTestCaseMgr) clientHandler.getContextVar("testCaseMgr");
            JSONObject loadParams = (JSONObject) clientHandler.getContextVar("loadParams");
            if (testCaseMgr == null || loadParams == null) {
                resObj.put("code", Constants.CODE_ERROR_NOT_LOAD);
            } else {
                Intent i = testCaseMgr.getTestCaseIntent(caseId);
                i.setClassName(loadParams.getString("pkgName"), loadParams.getString("compName"));
                Log.i(TAG, "Generated Intent=" + i + ", extra=" + (i.getExtras() != null ? i.getExtras().toString() : "null"));
                mController.startActivity(i);
                resObj.put("code", Constants.CODE_SUCCESS);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception when running test case #" + caseId);
            Log.e(TAG, Log.getStackTraceString(e));
            resObj.put("code", Constants.CODE_ERROR_START_COMPONENT);
        }
        clientHandler.sendResponse(resObj);
    }
}
