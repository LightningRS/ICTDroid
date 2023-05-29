package org.square16.ictdroid.rpc.interfaces;

import com.alibaba.fastjson.JSONObject;
import org.square16.ictdroid.rpc.RPCController;
import org.square16.ictdroid.rpc.annotations.RPCHandler;

public interface IRPCHandler extends Comparable<IRPCHandler> {
    public static final int ACTION_INIT = 1;
    public static final int ACTION_LOAD = 2;
    public static final int ACTION_RUN_CASE = 3;
    public static final int ACTION_RUN_CASE_RESULT = 4;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_ERROR_INVALID_SEQ = 1;
    public static final int CODE_ERROR_INVALID_REQ = 2;
    public static final int CODE_ERROR_INVALID_ACTION = 3;
    public static final int CODE_ERROR_NOT_LOAD = 4;
    public static final int CODE_ERROR_LOAD_FILE_NOT_FOUND = -1;
    public static final int CODE_ERROR_START_COMPONENT = -2;

    void handle(RPCController controller, JSONObject dataObj);

    @Override
    default int compareTo(IRPCHandler o) {
        RPCHandler thisInfo = this.getClass().getAnnotation(RPCHandler.class);
        RPCHandler otherInfo = o.getClass().getAnnotation(RPCHandler.class);
        if (thisInfo.priority() > otherInfo.priority()) {
            return 1;
        } else if (thisInfo.priority() < otherInfo.priority()) {
            return -1;
        } else {
            return thisInfo.name().compareTo(otherInfo.name());
        }
    }
}
