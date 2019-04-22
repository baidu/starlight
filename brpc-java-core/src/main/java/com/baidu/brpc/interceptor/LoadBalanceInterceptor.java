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

import java.util.concurrent.TimeUnit;

import com.baidu.brpc.Controller;
import com.baidu.brpc.client.AsyncAwareFuture;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
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
        Controller controller = request.getController();
        int currentTryTimes = 0;
        int maxTryTimes = rpcClient.getRpcClientOptions().getMaxTryTimes();
        while (currentTryTimes++ < maxTryTimes) {
            try {
                invokeRpc(request, response);
                break;
            } catch (RpcException ex) {
                exception = ex;
                if (exception.getCode() == RpcException.INTERCEPT_EXCEPTION) {
                    break;
                }
                // if application set the channel, brpc-java will not do retrying.
                // because application maybe send different request for different server instance.
                // this feature is used by Product Ads.
                if (controller != null && controller.getChannel() != null) {
                    break;
                }
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

    protected void rpcCore(Request request, Response response) throws Exception {
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
