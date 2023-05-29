package org.square16.ictdroid.testbridge.controller.rpc;

import android.os.Debug;
import android.util.Log;

import org.square16.ictdroid.testbridge.Constants;
import org.square16.ictdroid.testbridge.controller.TestController;
import org.square16.ictdroid.testbridge.controller.handlers.InitHandler;
import org.square16.ictdroid.testbridge.controller.handlers.LoadHandler;
import org.square16.ictdroid.testbridge.controller.handlers.RunCaseHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RPCServer {
    private static final String TAG = "RPCServer";
    private final TestController mController;
    private final RPCHandlerManager mHandlerManager;
    private ServerSocket ss;

    public RPCServer(TestController controller) {
        this.mController = controller;
        this.mHandlerManager = new RPCHandlerManager();
        this.ss = null;

        mHandlerManager.registerHandler(Constants.ACTION_INIT, new InitHandler(mController));
        mHandlerManager.registerHandler(Constants.ACTION_LOAD, new LoadHandler(mController));
        mHandlerManager.registerHandler(Constants.ACTION_RUN_CASE, new RunCaseHandler(mController));
    }

    public RPCHandlerManager getHandlerManager() {
        return mHandlerManager;
    }

    public TestController getController() {
        return mController;
    }

    public void close() {
        try {
            ss.close();
        } catch (IOException ignored) {
        }
        Log.i(TAG, "Test RPCServer closed");
        ss = null;
    }

    public void start() {
        try {
            this.ss = new ServerSocket(Debug.isDebuggerConnected() ? 9876 : 0);
            // this.ss = new ServerSocket(0);
            Log.i(TAG, "Test RPCServer started at port " + this.ss.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        new Thread("RPCServerThread") {
            @Override
            public void run() {
                Socket soc;
                try {
                    while (ss != null && !ss.isClosed()) {
                        soc = ss.accept();
                        new RPCClientHandler(RPCServer.this, soc).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
