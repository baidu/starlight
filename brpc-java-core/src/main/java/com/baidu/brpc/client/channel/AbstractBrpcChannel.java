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
package com.baidu.brpc.client.channel;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Queue;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Slf4j
public abstract class AbstractBrpcChannel implements BrpcChannel {
    protected String ip;
    protected int port;
    protected Bootstrap bootstrap;
    protected Protocol protocol;

    public AbstractBrpcChannel(String ip, int port, Bootstrap bootstrap, Protocol protocol) {
        this.ip = ip;
        this.port = port;
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
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        return port;
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
                .append(ip)
                .append(port)
                .toHashCode();
    }

    @Override
    public boolean equals(Object object) {
        boolean flag = false;
        if (object != null && BrpcChannel.class.isAssignableFrom(object.getClass())) {
            BrpcChannel rhs = (BrpcChannel) object;
            flag = new EqualsBuilder()
                    .append(ip, rhs.getIp())
                    .append(port, rhs.getPort())
                    .isEquals();
        }
        return flag;
    }
}
