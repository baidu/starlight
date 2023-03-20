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

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import io.netty.bootstrap.Bootstrap;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by liuruisen on 2020/8/24.
 */
public class ChannelPooledObjectFactoryTest {

    @Test
    public void validateObject() {
        ChannelPooledObjectFactory factory = new ChannelPooledObjectFactory(
            new PooledRpcChannelGroup(new URI.Builder("brpc://localhost:8999").build(), new Bootstrap()));
        RpcChannel mockChannel = Mockito.mock(RpcChannel.class);
        when(mockChannel.isActive()).thenReturn(true);
        PooledObject<RpcChannel> pooledObject = new DefaultPooledObject(mockChannel);
        assertTrue(factory.validateObject(pooledObject));
    }
}