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
 
package com.baidu.cloud.starlight.core.rpc.callback;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.sse.ServerEvent;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.core.rpc.sse.StarlightClientSseEmitter;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * client端 sse响应callback
 */
public class SseClientCallBack implements RpcCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(SseClientCallBack.class);

    private StarlightClientSseEmitter clientSseEmitter;

    private final Request request;

    private Timeout timeout;

    private RpcChannel rpcChannel;

    public SseClientCallBack(StarlightClientSseEmitter clientSseEmitter, Request request) {
        this.clientSseEmitter = clientSseEmitter;
        this.request = request;
        clientSseEmitter.setSseClientCallBack(this);
    }

    @Override
    public synchronized void onResponse(Response response) {
        if (response.getStatus() != Constants.SUCCESS_CODE) {
            if (response.getException() != null) {
                onError(response.getException());
            } else {
                onError(
                    new StarlightRpcException(response.getStatus(), response.getErrorMsg(), response.getException()));
            }
            return;
        }
        cancelTimeout();
        List<ServerEvent> serverEvents = (List<ServerEvent>) response.getResult();
        for (ServerEvent serverEvent : serverEvents) {
            if (serverEvent == ServerEvent.START_EVENT) {
                LOGGER.debug("receive sse response:{}", response);
                continue;
            } else if (serverEvent == ServerEvent.COMPLETE_EVENT) {
                LOGGER.debug("receive sse complete event:{}", response);
                clientSseEmitter.triggerOnCompletion();
                closeRpcChannel();
            } else {
                LOGGER.debug("receive sse servet event:{}", serverEvent);
                clientSseEmitter.pushServerEvent(serverEvent);
            }
        }
    }

    @Override
    public synchronized void onError(Throwable e) {
        cancelTimeout();
        clientSseEmitter.triggerOnError(e);
        closeRpcChannel();
    }

    @Override
    public void addTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void addRpcChannel(RpcChannel rpcChannel) {
        this.rpcChannel = rpcChannel;
    }

    public void closeRpcChannel() {
        if (rpcChannel != null) {
            // 从连接池删除连接
            rpcChannel.allCallbacks().clear();
            rpcChannel.removeAttribute(Constants.SSE_CALLBACK_ATTR_KEY);
            rpcChannel.removeAttribute(Constants.SSE_EMBEDDED_CHANNEL_KEY);
            rpcChannel.removeAttribute(Constants.SSE_REQUEST_ID_KEY);
            rpcChannel.getRpcChannelGroup().close();
        }
    }

    private void cancelTimeout() {
        if (timeout != null && !timeout.isCancelled()) {
            timeout.cancel();
        }
    }
}
