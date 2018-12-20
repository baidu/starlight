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

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.baidu.brpc.client.pool.ChannelPooledObjectFactory;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Protocol;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * BrpcChannelGroup class keeps fixed connections with one server
 * Created by wenweihu86 on 2017/9/29.
 */
@Slf4j
@Getter
@Setter
public class BrpcChannelGroup {
    private String ip;
    private int port;
    private GenericObjectPool<Channel> channelFuturePool;
    private volatile long failedNum;
    private int readTimeOut;
    private int latencyWindowSize;
    /**
     * Used to save the rpc latency of the recent several rpc calls.
     * This is for the fair load balance algorithm.
     */
    private Queue<Integer> latencyWindow;
    private Bootstrap bootstrap;
    private RpcClientOptions rpcClientOptions;
    private Protocol protocol;

    public BrpcChannelGroup(String ip, int port, RpcClient rpcClient) {
        this.ip = ip;
        this.port = port;
        this.bootstrap = rpcClient.getBootstrap();
        this.protocol = rpcClient.getProtocol();
        this.rpcClientOptions = rpcClient.getRpcClientOptions();
        this.readTimeOut = rpcClientOptions.getReadTimeoutMillis();
        this.latencyWindowSize = rpcClientOptions.getLatencyWindowSizeOfFairLoadBalance();
        this.latencyWindow = new ConcurrentLinkedQueue<Integer>();
        GenericObjectPoolConfig conf = new GenericObjectPoolConfig();
        // Maximum waiting time, when you need to borrow a connection, the maximum waiting time,
        // if the time is exceeded, throw an exception, -1 is no time limit
        conf.setMaxWaitMillis(rpcClientOptions.getConnectTimeoutMillis());
        conf.setMaxTotal(rpcClientOptions.getMaxTotalConnections());
        conf.setMaxIdle(rpcClientOptions.getMaxTotalConnections());
        conf.setMinIdle(rpcClientOptions.getMinIdleConnections());
        // Connect test when idle, start asynchronous evict thread for failure detection
        conf.setTestWhileIdle(true);
        // Maximum time for connection idle, testWhileIdle needs to be true
        conf.setTimeBetweenEvictionRunsMillis(5 * 60 * 1000);
        channelFuturePool = new GenericObjectPool<Channel>(
                new ChannelPooledObjectFactory(this, ip, port), conf);
        try {
            channelFuturePool.preparePool();
        } catch (Exception ex) {
            log.warn("init min idle object pool failed");
        }
    }

    public Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException {
        return channelFuturePool.borrowObject();
    }

    public void returnChannel(Channel channel) {
        try {
            channelFuturePool.returnObject(channel);
        } catch (Exception e) {
            log.debug("return channel failed:{}", e.getMessage());
        }
    }

    public void removeChannel(Channel channel) {
        try {
            channelFuturePool.invalidateObject(channel);
        } catch (Exception e) {
            log.debug("remove channel failed:{}", e.getMessage());
        }
    }

    public void close() {
        channelFuturePool.close();
    }

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
    public boolean equals(Object obj) {
        boolean flag = false;
        if (obj != null && BrpcChannelGroup.class.isAssignableFrom(obj.getClass())) {
            BrpcChannelGroup f = (BrpcChannelGroup) obj;
            flag = new EqualsBuilder()
                    .append(ip, f.getIp())
                    .append(port, f.getPort())
                    .isEquals();
        }
        return flag;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(ip)
                .append(port)
                .toHashCode();
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public long getFailedNum() {
        return failedNum;
    }

    public void incFailedNum() {
        this.failedNum++;
    }

    public Queue<Integer> getLatencyWindow() {
        return latencyWindow;
    }

    public void updateLatency(int latency) {
        latencyWindow.add(latency);
        if (latencyWindow.size() > latencyWindowSize) {
            latencyWindow.poll();
        }
    }

    public void updateLatencyWithReadTimeOut() {
        updateLatency(readTimeOut);
    }
}
