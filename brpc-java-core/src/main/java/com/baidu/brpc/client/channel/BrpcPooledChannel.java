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

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.client.pool.ChannelPooledObjectFactory;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * BrpcPooledChannelGroup class keeps fixed connections with one server
 * Created by wenweihu86 on 2017/9/29.
 */

@Slf4j
public class BrpcPooledChannel extends AbstractBrpcChannel {
    private GenericObjectPool<Channel> channelFuturePool;
    /**
     * failedNum only effect balanceStrategy
     * thread-unsafe type can be accepted, so not use AtomicLong which may affect performance partly
     */
    private volatile long failedNum;
    private int readTimeOut;
    private int latencyWindowSize;
    /**
     * Used to save the rpc latency of the recent several rpc calls.
     * This is for the fair load balance algorithm.
     */
    private Queue<Integer> latencyWindow;
    private RpcClientOptions rpcClientOptions;

    public BrpcPooledChannel(ServiceInstance serviceInstance, RpcClient rpcClient) {
        super(serviceInstance, rpcClient.getBootstrap(), rpcClient.getProtocol(), rpcClient);
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
        conf.setTimeBetweenEvictionRunsMillis(rpcClientOptions.getTimeBetweenEvictionRunsMillis());
        channelFuturePool = new GenericObjectPool<Channel>(new ChannelPooledObjectFactory(
                this, serviceInstance.getIp(), serviceInstance.getPort()), conf);
        try {
            channelFuturePool.preparePool();
        } catch (Exception ex) {
            log.warn("init min idle object pool failed");
        }
    }

    @Override
    public Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException {
        return channelFuturePool.borrowObject();
    }

    @Override
    public void returnChannel(Channel channel) {
        try {
            channelFuturePool.returnObject(channel);
        } catch (Exception e) {
            log.debug("return channel failed:{}", e.getMessage());
        }
    }

    @Override
    public void removeChannel(Channel channel) {
        try {
            channelFuturePool.invalidateObject(channel);
        } catch (Exception e) {
            log.debug("remove channel failed:{}", e.getMessage());
        }
    }

    @Override
    public void close() {
        channelFuturePool.close();
    }

    @Override
    public long getFailedNum() {
        return failedNum;
    }

    @Override
    public void incFailedNum() {
        this.failedNum++;
    }

    @Override
    public Queue<Integer> getLatencyWindow() {
        return latencyWindow;
    }

    @Override
    public void updateLatency(int latency) {
        latencyWindow.add(latency);
        if (latencyWindow.size() > latencyWindowSize) {
            latencyWindow.poll();
        }
    }


    @Override
    public void updateMaxConnection(int num) {

        channelFuturePool.setMaxTotal(num);
        channelFuturePool.setMaxIdle(num);

    }

    @Override
    public int getCurrentMaxConnection() {
        return channelFuturePool.getMaxTotal();
    }

    @Override
    public int getActiveConnectionNum() {
        return channelFuturePool.getNumActive();
    }

    @Override
    public int getIdleConnectionNum() {
        return channelFuturePool.getNumIdle();
    }

    @Override
    public void updateLatencyWithReadTimeOut() {
        updateLatency(readTimeOut);
    }

    public RpcClient getRpcClient() {
        return rpcClient;
    }

    public void setRpcClient(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

}
