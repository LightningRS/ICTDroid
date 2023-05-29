package org.square16.ictdroid.testbridge.controller.rpc;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.testbridge.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RPCClientHandler extends Thread {
    private static final String TAG = "RPCClientHandler";
    private final RPCServer mServer;
    private final Socket mSocket;
    private final DataInputStream mDis;
    private final DataOutputStream mDos;
    private Integer mSeq;

    private final Map<String, Object> mContextVars;

    public RPCClientHandler(RPCServer server, Socket soc) throws IOException {
        super("RPCClientHandler");
        this.mServer = server;
        this.mSocket = soc;
        this.mDis = new DataInputStream(soc.getInputStream());
        this.mDos = new DataOutputStream(soc.getOutputStream());
        this.mSeq = 0;
        this.mContextVars = new HashMap<>();
    }

    public void setContextVar(String key, Object val) {
        mContextVars.put(key, val);
    }

    public Object getContextVar(String key) {
        return mContextVars.get(key);
    }

    public void sendResponse(JSONObject resultObj) {
        resultObj.put("seq", mSeq);
        this.sendResponse(resultObj.toJSONString());
    }

    private void sendResponse(String result) {
        try {
            mDos.writeUTF(result);
            Log.d(TAG, "Sent Text: " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Log.i(TAG, "Start RPCClientHandler thread for [" + mSocket.getRemoteSocketAddress() + "]");
        while (true) {
            try {
                String recvText = mDis.readUTF();
                Log.d(TAG, "Received Text: " + recvText);
                JSONObject recvObj = JSON.parseObject(recvText);

                // Check sequence number
                if (!Objects.equals(recvObj.get("seq"), mSeq + 1)) {
                    JSONObject resObj = new JSONObject();
                    resObj.put("code", Constants.CODE_ERROR_INVALID_SEQ);
                    sendResponse(resObj);
                    continue;
                }
                mSeq++;
                Integer action = recvObj.getInteger("action");
                RPCHandler[] handlers = mServer.getHandlerManager().getHandlersByAction(action);
                if (handlers == null) {
                    JSONObject resObj = new JSONObject();
                    resObj.put("code", Constants.CODE_ERROR_INVALID_ACTION);
                    sendResponse(resObj);
                    continue;
                }
                for (RPCHandler handler : handlers) {
                    handler.handle(this, recvObj);
                }
            } catch (EOFException e) {
                Log.i(TAG, "Test RPCClient [" + mSocket.getRemoteSocketAddress() + "] disconnected");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Exception in RPCClientHandler");
                Log.e(TAG, Log.getStackTraceString(e));
                // break;
            }
        }
        Log.i(TAG, "RPCClientHandler thread for [" + mSocket.getRemoteSocketAddress() + "] ended");
    }
}
