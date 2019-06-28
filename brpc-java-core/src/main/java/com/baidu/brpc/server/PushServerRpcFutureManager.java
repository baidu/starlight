package com.baidu.brpc.server;

import com.baidu.brpc.client.FastFutureStore;
import com.baidu.brpc.client.RpcFuture;

public class PushServerRpcFutureManager {
    private static volatile PushServerRpcFutureManager instance;
    private FastFutureStore pendingRpc;

    public static PushServerRpcFutureManager getInstance() {
        if (instance == null) {
            synchronized(PushServerRpcFutureManager.class) {
                if (instance == null) {
                    instance = new PushServerRpcFutureManager();
                }
            }
        }
        return instance;
    }

    private PushServerRpcFutureManager() {
        pendingRpc = FastFutureStore.getInstance(0);
    }

    public long putRpcFuture(RpcFuture future) {
        long logId = pendingRpc.put(future);
        future.setLogId(logId);
        return logId;
    }

    public RpcFuture getRpcFuture(Long logId) {
        return pendingRpc.get(logId);
    }

    public RpcFuture removeRpcFuture(Long logId) {
        return pendingRpc.getAndRemove(logId);
    }
}