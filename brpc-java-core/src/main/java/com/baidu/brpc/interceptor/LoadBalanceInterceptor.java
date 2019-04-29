/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.brpc.interceptor;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.AsyncAwareFuture;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

import io.netty.channel.Channel;
import lombok.Setter;

/**
 * retry + load balance + rpc.
 * this interceptor is the last one of client interceptor list.
 * user can implement custom interceptor to replace it.
 */

@Setter
public class LoadBalanceInterceptor extends AbstractInterceptor {
    protected RpcClient rpcClient;

    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
        RpcException exception = null;
        int currentTryTimes = 0;
        int maxTryTimes = rpcClient.getRpcClientOptions().getMaxTryTimes();
        while (currentTryTimes < maxTryTimes) {
            try {
                // if it is a retry request, add the last selected instance to request,
                // so that load balance strategy can exclude the selected instance.
                // if it is the initial request, not init HashSet, so it is more fast.
                // therefore, it need LoadBalanceStrategy to judge if selectInstances is null.
                if (currentTryTimes > 0) {
                    if (request.getSelectedInstances() == null) {
                        request.setSelectedInstances(new HashSet<BrpcChannel>(maxTryTimes - 1));
                    }
                    BrpcChannel lastInstance = ChannelInfo
                            .getClientChannelInfo(request.getChannel()).getChannelGroup();
                    request.getSelectedInstances().add(lastInstance);
                }
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
        // select instance by load balance, and select channel from instance.
        Channel channel = rpcClient.selectChannel(request);
        request.setChannel(channel);
        return channel;
    }

    protected void rpcCore(Request request, Response response) throws Exception {
        // send request with the channel.
        AsyncAwareFuture future = rpcClient.sendRequest(request);
        if (future.isAsync()) {
            response.setRpcFuture((RpcFuture) future);
        } else {
            long readTimeout;
            RpcContext rpcContext = request.getRpcContext();
            if (rpcContext != null && rpcContext.getReadTimeoutMillis() != null) {
                readTimeout = rpcContext.getReadTimeoutMillis();
            } else {
                readTimeout = rpcClient.getRpcClientOptions().getReadTimeoutMillis();
            }
            response.setResult(future.get(readTimeout, TimeUnit.MILLISECONDS));
        }
    }
}
