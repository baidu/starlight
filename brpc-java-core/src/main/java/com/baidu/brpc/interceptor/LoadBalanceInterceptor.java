package com.baidu.brpc.interceptor;

import java.util.concurrent.TimeUnit;

import com.baidu.brpc.Controller;
import com.baidu.brpc.client.AsyncAwareFuture;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

import io.netty.channel.Channel;
import lombok.Setter;

@Setter
public class LoadBalanceInterceptor extends AbstractInterceptor {
    protected RpcClient rpcClient;

    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
        selectChannel(request);
        invokeRpc(request, response);
    }

    protected Channel selectChannel(Request request) {
        // if user set channel in controller, rpc will not select channel again.
        // otherwise select instance by load balance, and select channel from instance.
        Channel channel;
        Controller controller = request.getController();
        if (controller != null && controller.getChannel() != null) {
            channel = controller.getChannel();
        } else {
            channel = rpcClient.selectChannel();
        }
        request.setChannel(channel);
        return channel;
    }

    protected void invokeRpc(Request request, Response response) throws Exception {
        // send request with the channel.
        AsyncAwareFuture future = rpcClient.sendRequest(request);
        if (future.isAsync()) {
            response.setRpcFuture((RpcFuture) future);
        } else {
            long readTimeout;
            Controller controller = request.getController();
            if (controller != null && controller.getReadTimeoutMillis() != null) {
                readTimeout = controller.getReadTimeoutMillis();
            } else {
                readTimeout = rpcClient.getRpcClientOptions().getReadTimeoutMillis();
            }
            response.setResult(future.get(readTimeout, TimeUnit.MILLISECONDS));
        }
    }
}
