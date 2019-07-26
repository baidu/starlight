/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.brpc.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.utils.CollectionUtils;

import io.netty.util.Timeout;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("unchecked")
@Setter
@Getter
public class RpcFuture<T> implements AsyncAwareFuture<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcFuture.class);

    protected CountDownLatch latch;
    protected Timeout timeout;

    protected RpcCallback<T> callback;  // callback cannot be set after init
    protected ChannelInfo channelInfo;
    protected RpcClient rpcClient;
    protected RpcMethodInfo rpcMethodInfo;

    protected Response response;
    protected boolean isDone;
    // record the time of request
    // used in FAIR load balancing
    protected long startTime;
    protected long endTime;

    protected volatile long logId;

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
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, e);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "@logId = " + logId;
    }

    protected void setRpcContext() {
        if (response == null) {
            return;
        }
        if (response.getBinaryAttachment() != null
                || response.getKvAttachment() != null) {
            RpcContext rpcContext = RpcContext.getContext();
            if (response.getBinaryAttachment() != null) {
                rpcContext.setResponseBinaryAttachment(response.getBinaryAttachment());
            }
            if (response.getKvAttachment() != null) {
                rpcContext.setResponseKvAttachment(response.getKvAttachment());
            }
        }
    }
}