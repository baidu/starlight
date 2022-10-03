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
 
package com.baidu.cloud.starlight.transport.channel;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.thirdparty.netty.bootstrap.Bootstrap;
import com.baidu.cloud.thirdparty.pool2.PooledObject;
import com.baidu.cloud.thirdparty.pool2.impl.DefaultPooledObjectInfo;
import com.baidu.cloud.thirdparty.pool2.impl.GenericObjectPool;
import com.baidu.cloud.thirdparty.pool2.impl.GenericObjectPoolConfig;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * PooledRpcChannel: keep fixed connections with one server Created by liuruisen on 2020/3/31.
 */
public class PooledRpcChannelGroup extends NettyRpcChannelGroup {

    private GenericObjectPool<RpcChannel> channelPool;

    public PooledRpcChannelGroup(URI uri, Bootstrap bootstrap) {
        super(uri, bootstrap);
        // init();
    }

    @Override
    public RpcChannel getRpcChannel() {
        try {
            RpcChannel rpcChannel = channelPool.borrowObject();
            LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY)
                .set(rpcChannel.channel().id().asLongText(), Thread.currentThread().getContextClassLoader());
            return rpcChannel;
        } catch (Exception e) {
            throw new TransportException(TransportException.CONNECT_EXCEPTION,
                "Get Channel from pool failed :" + e.getMessage());
        }
    }

    /**
     * Return channel to channelPool
     * 
     * @param rpcChannel
     */
    @Override
    public void returnRpcChannel(RpcChannel rpcChannel) {
        try {
            channelPool.returnObject(rpcChannel);
        } catch (Exception e) {
            LOGGER.debug("Return channel failed:{} to PooledChannel", e.getMessage());
        }
    }

    /**
     * PooledChannel reconnect means invalidate channel and create new by GenericObjectPool
     * 
     * @param rpcChannel
     */
    @Override
    public void removeRpcChannel(RpcChannel rpcChannel) {
        try {
            channelPool.invalidateObject(rpcChannel);
        } catch (Exception e) {
            LOGGER.debug("Remove channel failed from PooledChannelGroup.", e);
        }
    }

    @Override
    public int rpcChannelCount() {
        return channelPool.listAllObjects().size();
    }

    /**
     * PooledChannel init means prepare ChannelPool
     */
    @Override
    public void init() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig
            .setMaxWaitMillis(getUri().getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.CONNECT_TIMEOUT_VALUE));
        poolConfig
            .setMaxTotal(getUri().getParameter(Constants.MAX_TOTAL_CONNECTIONS_KEY, Constants.MAX_TOTAL_CONNECTIONS));
        poolConfig
            .setMaxIdle(getUri().getParameter(Constants.MAX_IDLE_CONNECTIONS_KEY, Constants.MAX_IDLE_CONNECTIONS));
        poolConfig
            .setMinIdle(getUri().getParameter(Constants.MIN_IDLE_CONNECTIONS_KEY, Constants.MIN_IDLE_CONNECTIONS));
        // Connect test when idle, start asynchronous evict thread for failure detection
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(Constants.TIME_BETWEEN_EVICTION_RUN_MILLS);

        channelPool = new GenericObjectPool<>(new ChannelPooledObjectFactory(this), poolConfig);

        try {
            channelPool.preparePool();
        } catch (Exception e) {
            LOGGER.warn("Init min idle object pool failed");
        }
    }

    @Override
    public void close() {
        channelPool.close();
    }

    @Override
    public Set<RpcChannel> allRpcChannels() {
        Set<RpcChannel> rpcChannels = new HashSet<>();
        Set<DefaultPooledObjectInfo> pooledObjectInfos = channelPool.listAllObjects();
        try {
            for (DefaultPooledObjectInfo pooledObjectInfo : pooledObjectInfos) {
                Field pooledObjectField = pooledObjectInfo.getClass().getDeclaredField("pooledObject");
                pooledObjectField.setAccessible(true);
                PooledObject pooledObject = (PooledObject) pooledObjectField.get(pooledObjectInfo);
                rpcChannels.add((RpcChannel) pooledObject.getObject());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Get all rpcChannels from PooledChannelGroup failed", e);
        }

        return rpcChannels;
    }
}
