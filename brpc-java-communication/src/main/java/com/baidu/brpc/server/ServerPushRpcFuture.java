package com.baidu.brpc.server;

import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Response;

public class ServerPushRpcFuture extends RpcFuture {

    public void handleConnection(Response response) {
        this.response = response;
        this.endTime = System.currentTimeMillis();
        timeout.cancel();
        latch.countDown();
        isDone = true;
    }

    public void handleResponse(Response response) {
        handleConnection(response);
        if (isAsync()) {
            setRpcContext();
            if (response == null) {
                callback.fail(new RpcException(RpcException.SERVICE_EXCEPTION, "internal error"));
            } else if (response.getResult() != null) {
                callback.success(response.getResult());
            } else {
                callback.fail(response.getException());
            }
        }
    }

}
