package org.square16.ictdroid.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.square16.ictdroid.Constants;
import org.square16.ictdroid.TestController;
import org.square16.ictdroid.rpc.annotations.RPCHandler;
import org.square16.ictdroid.rpc.handler.InitRecvHandler;
import org.square16.ictdroid.rpc.handler.LoadRecvHandler;
import org.square16.ictdroid.rpc.handler.RunCaseRecvHandler;
import org.square16.ictdroid.rpc.interfaces.IRPCHandler;
import org.square16.ictdroid.utils.ADBInterface;
import org.square16.ictdroid.utils.SortedArrayList;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Slf4j
public class RPCController {
    private static final ADBInterface adb = ADBInterface.getInstance();
    private static final int MAX_RETRY = 3;
    private final TestController mTestController;
    private final SortedArrayList<IRPCHandler> mSeqHandlerList;
    private Socket mSoc;
    private DataInputStream mDis;
    private DataOutputStream mDos;
    private int mForwardPort;
    private int mSeq;
    private int mRetryCnt;
    private boolean mIsReady;
    private boolean mIsLoaded;
    // For unexpected reconnect
    private JSONObject mLoadCaseCommand;

    public RPCController(TestController testController, int forwardPort) {
        mTestController = testController;
        mForwardPort = forwardPort;
        mSeq = 0;
        mRetryCnt = 0;
        mIsReady = false;
        mSeqHandlerList = new SortedArrayList<>();
    }

    public TestController getTestController() {
        return mTestController;
    }

    public void setForwardPort(int port) {
        mForwardPort = port;
    }

    /* Persistent handlers (deprecated)
    private void registerPersistentHandlers() {
        mPersistentHandlerMap.clear();
        Reflections ref = new Reflections(getClass().getPackageName() + ".handler");
        Set<Class<?>> classes = ref.getTypesAnnotatedWith(RPCHandler.class);
        for (Class<?> clazz : classes) {
            if (!IRPCHandler.class.isAssignableFrom(clazz)) {
                Log.error("Handler [{}] is not implements IRPCHandler", clazz.getName());
                return;
            }
            RPCHandler handlerConfig = clazz.getAnnotation(RPCHandler.class);
            int action = handlerConfig.action();
            SortedArrayList<IRPCHandler> handlerList = mPersistentHandlerMap.get(action);
            if (handlerList == null) {
                handlerList = new SortedArrayList<>();
                mPersistentHandlerMap.put(action, handlerList);
            }
            try {
                handlerList.add((IRPCHandler) clazz.getDeclaredConstructor().newInstance());
                Log.info("Registered handler [{}]", handlerConfig.name());
            } catch (ReflectiveOperationException e) {
                Log.error("Cannot construct handler on class [{}]", clazz.getName(), e);
            }
        }
    }*/

    private void registerSeqHandler(IRPCHandler handler) {
        mSeqHandlerList.add(handler);
    }

    public void send(JSONObject dataObj) {
        while (mDos == null) {
            Thread.yield();
        }
        try {
            Integer action = dataObj.getInteger("action");
            if (action != null) {
                if (!action.equals(IRPCHandler.ACTION_INIT)) {
                    while (!mIsReady) {
                        Thread.yield();
                    }
                }
                if (action.equals(IRPCHandler.ACTION_LOAD)) {
                    mLoadCaseCommand = dataObj;
                } else if (action.equals(IRPCHandler.ACTION_RUN_CASE)) {
                    while (!mIsLoaded) {
                        Thread.yield();
                    }
                }
            }
            dataObj.put("seq", ++mSeq);
            String data = dataObj.toJSONString();
            mDos.writeUTF(data);
            log.debug("Socket send: {}", data);
        } catch (IOException e) {
            log.error("IOException when sending rpc data", e);
            disconnect(true);
        }
    }

    public void send(JSONObject dataObj, IRPCHandler handler) {
        this.registerSeqHandler(handler);
        this.send(dataObj);
    }

    private void receive() {
        try {
            while (mDis != null) {
                String data = mDis.readUTF();
                log.debug("Socket recv: {}", data);
                this.handle(data);
            }
        } catch (IOException e) {
            log.error("IOException when receiving data from socket", e);
            disconnect(true);
        }
    }

    public void init() {
        mSeq = 0;
        try {
            mSoc = new Socket("127.0.0.1", mForwardPort);
            mDis = new DataInputStream(mSoc.getInputStream());
            mDos = new DataOutputStream(mSoc.getOutputStream());
            Thread recvThread = new Thread("RPCReceiveThread") {
                @Override
                public void run() {
                    receive();
                }
            };
            recvThread.setDaemon(true);
            recvThread.start();
            mRetryCnt = 0;
            log.info("Connected to RPC on port {}", mForwardPort);

            // Send init message
            JSONObject dataObj = new JSONObject();
            dataObj.put("action", IRPCHandler.ACTION_INIT);
            this.send(dataObj, new InitRecvHandler());
        } catch (IOException e) {
            log.error("Failed to connect to remote rpc on port {}", mForwardPort);
        }
    }

    public void disconnect() {
        disconnect(false);
    }

    public void disconnect(boolean isRetry) {
        if (!mIsReady) {
            return;
        }
        mIsReady = false;
        mIsLoaded = false;

        // Announce the CompStateMonitor
        mTestController.getCompStateMonitor().setCompState(CompStateMonitor.STATE_CLIENT_ERROR);

        try {
            mDis.close();
            mDos.close();
            mSoc.close();
        } catch (IOException ignored) {

        }
        mSeqHandlerList.clear();
        mDis = null;
        mDos = null;
        mSoc = null;
        if (isRetry && mRetryCnt < MAX_RETRY) {
            mRetryCnt++;
            log.error("Disconnected from RPC unexpectedly, reconnect after 3 seconds...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("InterruptedException when reconnecting", e);
            }
            // Restart test bridge
            adb.forceStopApp(Constants.CLIENT_PKG_NAME);
            adb.startActivity(Constants.CLIENT_PKG_NAME, Constants.CLIENT_ACT_NAME);

            if (mLoadCaseCommand != null) {
                while (!mIsReady) {
                    Thread.yield();
                }
                this.send(mLoadCaseCommand, new LoadRecvHandler());
            }
        }
    }

    private void handle(String recvData) {
        try {
            JSONObject dataObj = (JSONObject) JSON.parse(recvData);
            if (!dataObj.containsKey("code") || !dataObj.containsKey("seq")) {
                log.error("Invalid received data: {}", dataObj.toJSONString());
                this.disconnect(true);
            }

            // Verify sequence number
            int rSeq = dataObj.getIntValue("seq");
            if (rSeq != mSeq) {
                // Sequence number not match, reconnect
                log.error("Invalid received seq! rSeq={}, mSeq={}", rSeq, mSeq);
                this.disconnect(true);
            }

            // Call handlers
            for (IRPCHandler handler : mSeqHandlerList) {
                RPCHandler handlerInfo = handler.getClass().getAnnotation(RPCHandler.class);
                try {
                    handler.handle(this, dataObj);
                } catch (Exception e) {
                    log.error("Exception when calling rpc handler [{}]", handlerInfo.name(), e);
                }
                if (handlerInfo.autoRemove()) {
                    mSeqHandlerList.remove(handler);
                }
            }
        } catch (JSONException e) {
            log.error("JSONException when parsing received data", e);
        }
    }

    public boolean isReady() {
        return mIsReady;
    }

    public void setReady(boolean isReady) {
        mIsReady = isReady;
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }

    public void setLoaded(boolean isLoaded) {
        mIsLoaded = isLoaded;
    }

    public void loadTestcase(String pkgName, String compName, String compType, String strategy) {
        JSONObject loadReq = new JSONObject();
        loadReq.put("action", IRPCHandler.ACTION_LOAD);
        JSONObject loadReqData = new JSONObject();
        loadReqData.put("pkgName", pkgName);
        loadReqData.put("compName", compName);
        loadReqData.put("compType", compType);
        loadReqData.put("strategy", strategy);
        loadReq.put("data", loadReqData);
        send(loadReq, new LoadRecvHandler());
    }

    public void reloadTestcase() {
        if (mLoadCaseCommand == null) {
            log.error("Cannot reload testcases! Missing mLoadCaseCommand");
            return;
        }
        send(mLoadCaseCommand, new LoadRecvHandler());
    }

    public void runTestcase(int caseId) {
        JSONObject runReq = new JSONObject();
        runReq.put("action", IRPCHandler.ACTION_RUN_CASE);
        JSONObject runReqData = new JSONObject();
        runReqData.put("caseId", caseId);
        runReq.put("data", runReqData);
        send(runReq, new RunCaseRecvHandler());
    }
}
