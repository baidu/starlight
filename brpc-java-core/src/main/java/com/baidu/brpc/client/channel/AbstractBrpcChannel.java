/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.brpc.client.channel;

import java.net.InetSocketAddress;
import java.util.Queue;

import com.baidu.brpc.client.instance.ServiceInstance;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBrpcChannel implements BrpcChannel {
    protected ServiceInstance serviceInstance;
    protected Bootstrap bootstrap;
    protected Protocol protocol;

    public AbstractBrpcChannel(ServiceInstance serviceInstance, Bootstrap bootstrap, Protocol protocol) {
        this.serviceInstance = serviceInstance;
        this.bootstrap = bootstrap;
        this.protocol = protocol;
    }

    @Override
    public void updateChannel(Channel channel) {
    }


    @Override
    public Channel connect(final String ip, final int port) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.debug("future callback, connect to {}:{} success, channel={}",
                            ip, port, channelFuture.channel());
                } else {
                    log.debug("future callback, connect to {}:{} failed due to {}",
                            ip, port, channelFuture.cause().getMessage());
                }
            }
        });
        future.syncUninterruptibly();
        if (future.isSuccess()) {
            return future.channel();
        } else {
            // throw exception when connect failed to the connection pool acquirer
            log.warn("connect to {}:{} failed, msg={}", ip, port, future.cause().getMessage());
            throw new RpcException(future.cause());
        }
    }

    @Override
    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    @Override
    public long getFailedNum() {
        return 0;
    }

    @Override
    public void incFailedNum() {

    }

    @Override
    public Queue<Integer> getLatencyWindow() {
        return null;
    }

    @Override
    public void updateLatency(int latency) {

    }

    @Override
    public void updateLatencyWithReadTimeOut() {

    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(serviceInstance.getIp())
                .append(serviceInstance.getPort())
                .toHashCode();
    }

    @Override
    public boolean equals(Object object) {
        boolean flag = false;
        if (object != null && BrpcChannel.class.isAssignableFrom(object.getClass())) {
            BrpcChannel rhs = (BrpcChannel) object;
            flag = new EqualsBuilder()
                    .append(serviceInstance.getIp(), rhs.getServiceInstance().getIp())
                    .append(serviceInstance.getPort(), rhs.getServiceInstance().getPort())
                    .isEquals();
        }
        return flag;
    }
}
