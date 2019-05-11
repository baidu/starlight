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

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.utils.CollectionUtils;
import io.netty.util.Timeout;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@Setter
@Getter
public class RpcFuture<T> implements AsyncAwareFuture<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcFuture.class);

    private CountDownLatch latch;
    private Timeout timeout;

    private RpcCallback<T> callback;  // callback cannot be set after init
    private ChannelInfo channelInfo;
    private RpcClient rpcClient;
    private RpcMethodInfo rpcMethodInfo;
    private RpcContext rpcContext;

    private Response response;
    private boolean isDone;
    // record the time of request
    // used in FAIR load balancing
    private long startTime;
    private long endTime;

    private volatile long logId;

    public RpcFuture() {
        this.latch = new CountDownLatch(1);
        this.startTime = System.currentTimeMillis();
    }

    public RpcFuture(long logId) {
        this.logId = logId;
        this.latch = new CountDownLatch(1);
        this.startTime = System.currentTimeMillis();
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

    public void handleConnection(Response response) {
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

        timeout.cancel();
        latch.countDown();
        isDone = true;
    }

    public void handleResponse(Response response) {
        handleConnection(response);
        // invoke the chain of interceptors when async scene
        if (isAsync() && CollectionUtils.isNotEmpty(rpcClient.getInterceptors())) {
            int length = rpcClient.getInterceptors().size();
            for (int i = length - 1; i >= 0; i--) {
                rpcClient.getInterceptors().get(i).handleResponse(response);
            }
        }

        if (isAsync()) {
            setRpcContext();
            if (response == null) {
                callback.fail(new RpcException(RpcException.SERVICE_EXCEPTION, "internal error"));
            } else if (response.getResult() != null) {
                callback.success((T) response.getResult());
            } else {
                callback.fail(response.getException());
            }
        }
    }

    @Override
    public boolean isAsync() {
        return callback != null;
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
        setRpcContext();
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
            setRpcContext();
            return (T) response.getResult();
        } catch (InterruptedException e) {
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "@logId = " + logId;
    }

    private void setRpcContext() {
        if (response == null) {
            return;
        }
        if (response.getBinaryAttachment() != null
                || response.getKvAttachment() != null) {
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.reset();
            if (response.getBinaryAttachment() != null) {
                rpcContext.setResponseBinaryAttachment(response.getBinaryAttachment());
            }
            if (response.getKvAttachment() != null) {
                rpcContext.setResponseKvAttachment(response.getKvAttachment());
            }
        }
    }
}