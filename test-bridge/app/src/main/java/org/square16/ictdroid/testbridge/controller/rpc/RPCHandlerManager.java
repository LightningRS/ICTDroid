package org.square16.ictdroid.testbridge.controller.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class RPCHandlerManager {
    private static final Integer DEFAULT_PRIORITY = 10;
    private final Map<Integer, SortedMap<Integer, RPCHandler>> mHandlerMap;

    public RPCHandlerManager() {
        this.mHandlerMap = new HashMap<>();
    }

    public RPCHandler[] getHandlersByAction(Integer action) {
        SortedMap<Integer, RPCHandler> handlers = this.mHandlerMap.get(action);
        if (handlers == null) return null;
        return handlers.values().toArray(new RPCHandler[0]);
    }

    public void registerHandler(Integer action, RPCHandler handler) {
        SortedMap<Integer, RPCHandler> handlers = this.mHandlerMap.get(action);
        if (handlers == null) {
            handlers = new TreeMap<>();
            this.mHandlerMap.put(action, handlers);
        }
        handlers.put(DEFAULT_PRIORITY, handler);
    }

    public void registerHandler(Integer action, RPCHandler handler, Integer priority) {
        SortedMap<Integer, RPCHandler> handlers = this.mHandlerMap.get(action);
        if (handlers == null) {
            handlers = new TreeMap<>();
            this.mHandlerMap.put(action, handlers);
        }
        handlers.put(priority, handler);
    }
}
