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
 
package com.baidu.cloud.starlight.transport;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.transport.ServerPeer;
import com.baidu.cloud.starlight.transport.netty.NettyClient;
import com.baidu.cloud.starlight.transport.netty.NettyServer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/19.
 */
public class StarlightTransportFactoryTest {

    private StarlightTransportFactory transportFactory = new StarlightTransportFactory();

    @Test
    public void client() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8888);
        ClientPeer peer = transportFactory.client(builder.build());
        Assert.assertTrue(peer instanceof NettyClient);
    }

    @Test
    public void server() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8888);
        ServerPeer peer = transportFactory.server(builder.build());
        Assert.assertTrue(peer instanceof NettyServer);

    }
}