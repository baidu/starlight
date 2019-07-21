package com.baidu.brpc.server;

import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Response;

public class ServerPushRpcFuture extends RpcFuture {

    public void handleConnection(Response response) {
        this.response = response;
        this.endTime = System.currentTimeMillis();

        // only long connection need to update channel group
        //        if (rpcClient.isLongConnection()) {
        //            if (response != null && response.getResult() != null) {
        //                channelInfo.getChannelGroup().updateLatency((int) (endTime - startTime));
        //                channelInfo.handleResponseSuccess();
        //            } else {
        //                channelInfo.getChannelGroup().updateLatencyWithReadTimeOut();
        //                channelInfo.handleResponseFail();
        //            }
        //        } else {
        //            channelInfo.getChannelGroup().close();
        //        }

        timeout.cancel();
        latch.countDown();
        isDone = true;
    }

    public void handleResponse(Response response) {
        handleConnection(response);
        // invoke the chain of interceptors when async scene
        //        if (isAsync() && CollectionUtils.isNotEmpty(rpcClient.getInterceptors())) {
        //            int length = rpcClient.getInterceptors().size();
        //            for (int i = length - 1; i >= 0; i--) {
        //                rpcClient.getInterceptors().get(i).handleResponse(response);
        //            }
        //        }

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
