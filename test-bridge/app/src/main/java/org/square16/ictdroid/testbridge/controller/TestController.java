package org.square16.ictdroid.testbridge.controller;

import android.content.Intent;

import org.square16.ictdroid.testbridge.activities.BasicActivity;
import org.square16.ictdroid.testbridge.controller.rpc.RPCServer;
import org.square16.ictdroid.testbridge.utils.CSVTestCaseMgr;

public class TestController {
    private static final String TAG = "TestController";
    private final BasicActivity mActivity;
    private final RPCServer mServer;
    private CSVTestCaseMgr mTestCaseMgr;

    public TestController(BasicActivity activity) {
        this.mActivity = activity;
        this.mServer = new RPCServer(this);
    }

    public void init() {
        mServer.start();
    }

    public CSVTestCaseMgr getTestCaseMgr() {
        return mTestCaseMgr;
    }

    public void startActivity(Intent i) {
        // i.setClassName(mActivity, i.getComponent().getClassName());
        mActivity.startActivityForResult(i, 64206);
    }
}
