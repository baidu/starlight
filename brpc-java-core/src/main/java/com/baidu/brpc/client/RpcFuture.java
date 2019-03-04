/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.client;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcContext;

import io.netty.buffer.ByteBuf;
import io.netty.util.Timeout;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("unchecked")
@Setter
@Getter
public class RpcFuture<T> implements Future<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcFuture.class);

    private CountDownLatch latch;
    private Timeout timeout;
    private RpcCallback<T> callback;
    private ChannelInfo channelInfo;
    private RpcClient rpcClient;
    private RpcMethodInfo rpcMethodInfo;

    private Response response;
    private Map<String, String> kvAttachment;
    private ByteBuf binaryAttachment;
    private boolean isDone;
    // record the time of request
    // used in FAIR load balancing
    private long startTime;
    private long endTime;

    private volatile long logId;

    public RpcFuture() {
    }

    public RpcFuture(long logId) {
        this.logId = logId;
    }

    public RpcFuture(Timeout timeout,
                     RpcMethodInfo rpcMethodInfo,
                     RpcCallback<T> callback,
                     ChannelInfo channelInfo,
                     RpcClient rpcClient) {
        init(timeout, rpcMethodInfo, callback, channelInfo, rpcClient);
    }

    public void init(Timeout timeout,
                     RpcMethodInfo rpcMethodInfo,
                     RpcCallback<T> callback,
                     ChannelInfo channelInfo,
                     RpcClient rpcClient) {
        this.timeout = timeout;
        this.rpcMethodInfo = rpcMethodInfo;
        this.callback = callback;
        this.channelInfo = channelInfo;
        this.latch = new CountDownLatch(1);
        this.startTime = System.currentTimeMillis();
        this.rpcClient = rpcClient;
    }

    protected void processConnection(Response response) {
        this.response = response;
        this.endTime = System.currentTimeMillis();

        // only long connection need to update channel group
        if (rpcClient.isLongConnection()) {
            if (response != null && response.getResult() != null) {
                channelInfo.getChannelGroup().updateLatency((int) (endTime - startTime));
                channelInfo.handleResponseSuccess();
            } else {
                channelInfo.getChannelGroup().updateLatencyWithReadTimeOut();
                channelInfo.handleResponseFail();
            }
        } else {
            channelInfo.getChannelGroup().close();
        }
    }

    public void handleResponse(Response response) {
        processConnection(response);
        RpcContext rpcContext = RpcContext.getContext();
        if (response != null) {
            rpcContext.setResponseBinaryAttachment(response.getBinaryAttachment());
            rpcContext.setResponseKvAttachment(response.getKvAttachment());
            this.kvAttachment = response.getKvAttachment();
            this.binaryAttachment = response.getBinaryAttachment();
        }

        try {
            timeout.cancel();
            latch.countDown();
            // invoke the chain of interceptors
            if (CollectionUtils.isNotEmpty(rpcClient.getInterceptors())) {
                int length = rpcClient.getInterceptors().size();
                for (int i = length - 1; i >= 0; i--) {
                    rpcClient.getInterceptors().get(i).handleResponse(response);
                }
            }
            isDone = true;

            if (callback != null) {
                if (response == null) {
                    callback.fail(new RpcException(RpcException.SERVICE_EXCEPTION, "internal error"));
                } else if (response.getResult() != null) {
                    callback.success((T) response.getResult());
                } else {
                    callback.fail(response.getException());
                }
            }
        } finally {
            RpcContext.removeContext();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public T get() throws InterruptedException {
        latch.await();
        if (response == null) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION);
        }
        if (response.getException() != null) {
            throw new RpcException(response.getException());
        }
        RpcContext rpcContext = RpcContext.getContext();
        rpcContext.setResponseBinaryAttachment(binaryAttachment);
        rpcContext.setRequestKvAttachment(kvAttachment);
        return (T) response.getResult();
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        try {
            boolean ret = latch.await(timeout, unit);
            if (!ret || response == null) {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION);
            }
            if (response.getException() != null) {
                throw new RpcException(response.getException());
            }
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setResponseBinaryAttachment(binaryAttachment);
            rpcContext.setRequestKvAttachment(kvAttachment);
            return (T) response.getResult();
        } catch (InterruptedException e) {
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "@logId = " + logId;
    }
}