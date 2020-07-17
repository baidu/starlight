/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.brpc.client;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.utils.CollectionUtils;
import io.netty.util.Timeout;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@Setter
@Getter
public class RpcFuture<T> implements AsyncAwareFuture<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcFuture.class);

    protected CountDownLatch latch;
    protected Timeout timeout;

    protected Request request;
    protected RpcCallback<T> callback;  // callback cannot be set after init
    protected ChannelInfo channelInfo;
    protected ChannelType channelType;
    protected RpcMethodInfo rpcMethodInfo;

    protected Response response;
    protected boolean isDone;
    // record the time of request
    // used in FAIR load balancing
    protected long startTime;
    protected long endTime;

    protected volatile long correlationId;
    protected List<Interceptor> interceptors = new ArrayList<Interceptor>();

    public RpcFuture() {
        this.latch = new CountDownLatch(1);
        this.startTime = System.currentTimeMillis();
    }

    public RpcFuture(long correlationId) {
        this.correlationId = correlationId;
        this.latch = new CountDownLatch(1);
        this.startTime = System.currentTimeMillis();
    }

    public static RpcFuture createRpcFuture(Request request) {
        // create RpcFuture object
        RpcFuture rpcFuture = new RpcFuture();
        rpcFuture.setRpcMethodInfo(request.getRpcMethodInfo());
        rpcFuture.setCallback(request.getCallback());
        rpcFuture.setRequest(request);
        // generate correlationId
        FastFutureStore.getInstance(0).put(rpcFuture);
        return rpcFuture;
    }

    public void init(Timeout timeout,
                     RpcMethodInfo rpcMethodInfo,
                     RpcCallback<T> callback,
                     ChannelInfo channelInfo) {
        this.timeout = timeout;
        this.rpcMethodInfo = rpcMethodInfo;
        this.callback = callback;
        this.channelInfo = channelInfo;
        this.latch = new CountDownLatch(1);
        this.startTime = System.currentTimeMillis();
    }

    public void handleConnection(Response response) {
        this.response = response;
        this.endTime = System.currentTimeMillis();

        // only long connection need to update channel group
        if (channelType == ChannelType.SHORT_CONNECTION) {
            channelInfo.close();
        } else {
            if (response != null && (response.getResult() != null || response.isHeartbeat())) {
                channelInfo.getChannelGroup().updateLatency((int) (endTime - startTime));
                channelInfo.handleResponseSuccess();
            } else {
                channelInfo.getChannelGroup().updateLatencyWithReadTimeOut();
                channelInfo.handleResponseFail();
            }
        }

        timeout.cancel();
        latch.countDown();
        isDone = true;
    }

    public void handleResponse(Response response) {
        handleConnection(response);
        // invoke the chain of interceptors when async scene
        if (isAsync() && CollectionUtils.isNotEmpty(interceptors)) {
            int length = interceptors.size();
            for (int i = length - 1; i >= 0; i--) {
                interceptors.get(i).handleResponse(response);
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
        if (response != null && response.getException() != null) {
            throw new RpcException(response.getException());
        }
        if (response == null) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION);
        }
        setRpcContext();
        return (T) response.getResult();
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        try {
            boolean ret = latch.await(timeout, unit);
            if (!ret) {
                try {
                    if (request.getChannel() != null && request.getChannel().remoteAddress() instanceof InetSocketAddress) {
                        LOG.error("RpcFuture.get timeout, channel ip : " + ((InetSocketAddress) request.getChannel().remoteAddress()).getAddress().getHostAddress());
                    }
                } catch (Throwable e) {
                    LOG.error("RpcFuture.get log error", e);
                }

                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout");
            }
            assert response != null;
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
        return super.toString() + "@correlationId = " + correlationId;
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