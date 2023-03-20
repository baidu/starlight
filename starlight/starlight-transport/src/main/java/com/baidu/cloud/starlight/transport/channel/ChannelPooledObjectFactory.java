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

import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Channel pool factory used in rpc client to produce Channel Created by liuruisen on 2020/3/31.
 */
public class ChannelPooledObjectFactory extends BasePooledObjectFactory<RpcChannel> {

    private final NettyRpcChannelGroup rpcChannelGroup;

    public ChannelPooledObjectFactory(NettyRpcChannelGroup rpcChannelGroup) {
        this.rpcChannelGroup = rpcChannelGroup;
    }

    @Override
    public RpcChannel create() throws Exception {
        return new LongRpcChannel(rpcChannelGroup.connect(), ChannelSide.CLIENT, rpcChannelGroup);
    }

    @Override
    public PooledObject<RpcChannel> wrap(RpcChannel obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<RpcChannel> p) throws Exception {
        RpcChannel channel = p.getObject();
        new Thread(channel::close).start();
    }

    /**
     * Ensure the RpcChannel is safe to be retured by the pool
     * 
     * @param p
     * @return
     */
    @Override
    public boolean validateObject(PooledObject<RpcChannel> p) {
        RpcChannel channel = p.getObject();
        return channel.isActive();
    }
}
