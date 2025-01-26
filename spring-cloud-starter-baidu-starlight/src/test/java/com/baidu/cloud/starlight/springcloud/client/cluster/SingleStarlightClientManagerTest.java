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
 
package com.baidu.cloud.starlight.springcloud.client.cluster;

import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/12/7.
 */
public class SingleStarlightClientManagerTest {

    private static final String HOST = "localhost";

    private static final Integer PORT = 55555;

    private final TransportConfig transportConfig;


    public SingleStarlightClientManagerTest() {
        this.transportConfig = new TransportConfig();
    }

    @Test
    public void getSingleClient() {
        SingleStarlightClientManager clientManager = SingleStarlightClientManager.getInstance();
        clientManager.removeSingleClient(HOST, PORT);
        SingleStarlightClient singleStarlightClient = clientManager.getSingleClient(HOST, PORT);
        assertNull(singleStarlightClient);
    }

    @Test
    public void getOrCreateSingleClient() {
        SingleStarlightClientManager clientManager = SingleStarlightClientManager.getInstance();
        SingleStarlightClientManager starlightClientManager = Mockito.spy(clientManager);
        SingleStarlightClient singleStarlightClient = Mockito.mock(SingleStarlightClient.class);
        doReturn(singleStarlightClient).when(starlightClientManager).createSingleClient(HOST, PORT, transportConfig);

        starlightClientManager.getOrCreateSingleClient(HOST, PORT, transportConfig);

        assertEquals(1, clientManager.allSingleClients().size());
    }

    @Test
    public void destroyAll() {
        SingleStarlightClientManager clientManager = SingleStarlightClientManager.getInstance();
        SingleStarlightClientManager starlightClientManager = Mockito.spy(clientManager);

        SingleStarlightClient singleStarlightClient = Mockito.mock(SingleStarlightClient.class);
        doReturn(true).when(singleStarlightClient).isActive();
        doNothing().when(singleStarlightClient).destroy();
        doReturn(singleStarlightClient).when(starlightClientManager).createSingleClient(HOST, PORT, transportConfig);

        starlightClientManager.getOrCreateSingleClient(HOST, PORT, transportConfig);

        assertEquals(1, starlightClientManager.allSingleClients().size());

        clientManager.destroyAll();

        assertEquals(0, starlightClientManager.allSingleClients().size());
    }
}