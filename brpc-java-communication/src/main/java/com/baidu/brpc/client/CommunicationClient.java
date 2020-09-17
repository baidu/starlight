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

package com.baidu.brpc.client;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.client.channel.BrpcChannelFactory;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.ClientInvokeInterceptor;
import com.baidu.brpc.interceptor.ClientTraceInterceptor;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.thread.TimerInstance;
import com.baidu.brpc.utils.CollectionUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by huwenwei on 2019/12/06.
 */
@SuppressWarnings("unchecked")
@Getter
@Slf4j
public class CommunicationClient {
    protected ServiceInstance serviceInstance;
    protected CommunicationOptions communicationOptions;
    protected BrpcChannel brpcChannel;
    protected List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private AtomicBoolean stop = new AtomicBoolean(false);

    public CommunicationClient(
            ServiceInstance serviceInstance,
            CommunicationOptions communicationOptions,
            List<Interceptor> interceptors) {
        this.serviceInstance = serviceInstance;
        this.communicationOptions = communicationOptions.clone();
        this.brpcChannel = BrpcChannelFactory.createChannel(serviceInstance, this.communicationOptions);
        if (CollectionUtils.isNotEmpty(interceptors)) {
            this.interceptors.addAll(interceptors);
        }
        this.interceptors.add(new ClientTraceInterceptor());
        this.interceptors.add(new ClientInvokeInterceptor(this));
    }

    public void execute(Request request, Response response) throws RpcException {
        request.setCommunicationClient(this);
        Channel channel = selectChannel();
        request.setChannel(channel);
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
        RpcFuture rpcFuture = RpcFuture.createRpcFuture(request);
        if (request.getCallback() != null) {
            rpcFuture.setInterceptors(interceptors);
        }
        channelInfo.setCorrelationId(rpcFuture.getCorrelationId());
        rpcFuture.setChannelInfo(channelInfo);
        rpcFuture.setChannelType(communicationOptions.getChannelType());
        request.setRpcFuture(rpcFuture);
        request.setCorrelationId(rpcFuture.getCorrelationId());

        try {
            request.setSendBuf(communicationOptions.getProtocol().encodeRequest(request));
        } catch (Throwable t) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, t.getMessage(), t);
        }

        // register timeout timer
        Timer timeoutTimer = TimerInstance.getInstance();
        RpcTimeoutTimer timeoutTask = new RpcTimeoutTimer(
                channelInfo, request.getCorrelationId(), communicationOptions.getProtocol());
        Timeout timeout = timeoutTimer.newTimeout(timeoutTask, request.getReadTimeoutMillis(), TimeUnit.MILLISECONDS);
        request.getRpcFuture().setTimeout(timeout);
        try {
            // netty will release the send buffer after sent.
            // we retain here, so it can be used when rpc retry.
            request.retain();
            ChannelFuture sendFuture = request.getChannel().writeAndFlush(request.getSendBuf());
            sendFuture.awaitUninterruptibly(request.getWriteTimeoutMillis());
            if (!sendFuture.isSuccess()) {
                if (!(sendFuture.cause() instanceof ClosedChannelException)) {
                    log.warn("send request failed, channelActive={}, ex={}",
                            request.getChannel().isActive(), sendFuture.cause());
                }
                String errMsg = String.format("send request failed, channelActive=%b",
                        request.getChannel().isActive());
                throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
            }
        } catch (Exception ex) {
            channelInfo.handleRequestFail(communicationOptions.getChannelType(), request.getCorrelationId());
            timeout.cancel();
            log.debug("send request failed:", ex);
            if (ex instanceof RpcException) {
                throw (RpcException) ex;
            } else {
                throw new RpcException(RpcException.NETWORK_EXCEPTION, "send request failed", ex);
            }
        }

        // return channel
        channelInfo.handleRequestSuccess(communicationOptions.getChannelType());

        // receive
        if (rpcFuture.isAsync()) {
            response.setRpcFuture(rpcFuture);
        } else {
            response.setResult(rpcFuture.get(request.getReadTimeoutMillis(), TimeUnit.MILLISECONDS));
            response.setCorrelationId(rpcFuture.getCorrelationId());
        }
    }

    public void executeChain(Request request, Response response) throws RpcException {
        execute(request, response);
    }

    public Channel selectChannel() {
        Channel channel;
        try {
            channel = brpcChannel.getChannel();
        } catch (NoSuchElementException full) {
            int maxConnections = brpcChannel.getCurrentMaxConnection() * 2;
            brpcChannel.updateMaxConnection(maxConnections);
            String errMsg = String.format("channel pool is exhausted, and double maxTotalConnection,server=%s:%d",
                    brpcChannel.getServiceInstance().getIp(), brpcChannel.getServiceInstance().getPort());
            log.debug(errMsg);
            throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg, full);
        } catch (IllegalStateException illegalState) {
            String errMsg = String.format("channel pool is closed, server=%s:%d",
                    brpcChannel.getServiceInstance().getIp(), brpcChannel.getServiceInstance().getPort());
            log.debug(errMsg);
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, errMsg, illegalState);
        } catch (Exception connectedFailed) {
            String errMsg;
            if (communicationOptions.getChannelType() == ChannelType.POOLED_CONNECTION) {
                errMsg = String.format("channel pool make new object failed, "
                                + "active=%d,idle=%d,server=%s:%d, ex=%s",
                        brpcChannel.getActiveConnectionNum(),
                        brpcChannel.getIdleConnectionNum(),
                        brpcChannel.getServiceInstance().getIp(),
                        brpcChannel.getServiceInstance().getPort(),
                        connectedFailed.getMessage());
            } else {
                errMsg = String.format("get channel failed, ex=%s", connectedFailed.getMessage());
            }
            log.debug(errMsg);
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, errMsg, connectedFailed);
        }

        if (channel == null) {
            String errMsg = "channel is null, retry another channel";
            log.debug(errMsg);
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, errMsg);
        }
        if (!channel.isActive()) {
            brpcChannel.incFailedNum();
            // 如果连接不是有效的，从连接池中剔除。
            brpcChannel.removeChannel(channel);
            String errMsg = "channel is non active, retry another channel";
            throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
        }
        return channel;
    }

    public void stop() {
        if (stop.compareAndSet(false, true)) {
            if (brpcChannel != null) {
                brpcChannel.close();
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        boolean flag = false;
        if (object != null && CommunicationClient.class.isAssignableFrom(object.getClass())) {
            CommunicationClient rhs = (CommunicationClient) object;
            flag = new EqualsBuilder()
                    .append(serviceInstance, rhs.serviceInstance)
                    .isEquals();
        }
        return flag;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(serviceInstance)
                .toHashCode();
    }

    @Override
    public String toString() {
        return serviceInstance.toString();
    }

}
