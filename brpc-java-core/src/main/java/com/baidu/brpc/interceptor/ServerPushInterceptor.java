/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.brpc.interceptor;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.client.AsyncAwareFuture;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.ChannelManager;
import com.baidu.brpc.server.RpcServer;

import io.netty.channel.Channel;
import lombok.Setter;

/**
 * retry + load balance + rpc.
 * this interceptor is the last one of client interceptor list.
 * user can implement custom interceptor to replace it.
 */
@Setter
public class ServerPushInterceptor extends AbstractInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(ServerPushInterceptor.class);

    protected RpcServer rpcServer;

    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
        RpcException exception = null;
        int currentTryTimes = 0;
        int maxTryTimes = rpcServer.getRpcServerOptions().getMaxTryTimes();
        while (currentTryTimes < maxTryTimes) {
            try {

                //   if (currentTryTimes > 0) {
                //                    if (request.getChannel() != null) {
                //                        if (request.getSelectedInstances() == null) {
                //                            request.setSelectedInstances(new HashSet<BrpcChannel>(maxTryTimes -
                // 1));
                //                        }
                //                        BrpcChannel lastInstance = ChannelInfo
                //                                .getClientChannelInfo(request.getChannel()).getChannelGroup();
                //                        request.getSelectedInstances().add(lastInstance);
                //                    }
                //  }
                invokeRpc(request, response);
                break;
            } catch (RpcException ex) {
                exception = ex;
                if (exception.getCode() == RpcException.INTERCEPT_EXCEPTION) {
                    break;
                }
            } finally {
                currentTryTimes++;
            }
        }
        if (response.getResult() == null && response.getRpcFuture() == null) {
            if (exception == null) {
                exception = new RpcException(RpcException.UNKNOWN_EXCEPTION, "unknown error");
            }
            response.setException(exception);
        }
    }

    protected void invokeRpc(Request request, Response response) throws Exception {
        selectChannel(request);
        rpcCore(request, response);
    }

    protected Channel selectChannel(Request request) {
        // select instance by server push
        ChannelManager channelManager = ChannelManager.getInstance();
        String clientName = request.getClientName();
        Channel channel = channelManager.getChannel(clientName);
        if (channel == null) {
            LOG.error("cannot find a valid channel by name:" + clientName);
            throw new RpcException("cannot find a valid channel by name:" + clientName);
        }
        request.setChannel(channel);
        return channel;
    }

    protected void rpcCore(Request request, Response response) throws Exception {
        // send request with the channel.
        AsyncAwareFuture future = rpcServer.sendServerPush(request);
        if (future.isAsync()) {
            response.setRpcFuture((RpcFuture) future);
        } else {
            response.setResult(future.get(request.getReadTimeoutMillis(), TimeUnit.MILLISECONDS));
        }
    }

    public RpcServer getRpcServer() {
        return rpcServer;
    }

    public void setRpcServer(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }

}
