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
 
package com.baidu.cloud.starlight.springcloud.client.shutdown;

import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/4/26.
 */
public class ShutdownServerListFilterTest {

    private List<ServiceInstance> originalList;

    private SingleStarlightClientManager clientManager = SingleStarlightClientManager.getInstance();

    @Before
    public void before() {
        originalList = new LinkedList<>();
        for (int i = 0; i < 10000; i++) {
            long shutdownTime = System.currentTimeMillis();
            DefaultServiceInstance server = null;
            if (i < 2000) { // mark server as shutting down, shutdown time > epoch will remove
                server = new DefaultServiceInstance(i + "", "testApp", "localhost", 1000 + i, true,
                    Collections.singletonMap(SpringCloudConstants.EPOCH_KEY, String.valueOf(shutdownTime - 10)));
                SingleStarlightClient singleClient =
                    clientManager.getOrCreateSingleClient(server.getHost(), server.getPort(), new TransportConfig());
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.SHUTTING_DOWN, shutdownTime));
            }

            if (i >= 2000 && i < 4000) { // mark server as shutdown, shutdown time < epoch will not remove
                server = new DefaultServiceInstance(i + "", "testApp", "localhost", 1000 + i, true,
                    Collections.singletonMap(SpringCloudConstants.EPOCH_KEY, String.valueOf(shutdownTime + 10)));
                SingleStarlightClient singleClient =
                    clientManager.getOrCreateSingleClient(server.getHost(), server.getPort(), new TransportConfig());
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.SHUTDOWN, shutdownTime));
            }

            if (i >= 4000) {
                server = new DefaultServiceInstance(i + "", "testApp", "localhost", 1000 + i, true,
                    Collections.singletonMap(SpringCloudConstants.EPOCH_KEY, String.valueOf(shutdownTime - 10)));
                SingleStarlightClient singleClient =
                    clientManager.getOrCreateSingleClient(server.getHost(), server.getPort(), new TransportConfig());
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));
            }
            originalList.add(server);
        }

    }

    @Test
    public void getFilteredList() {
        ShutdownServerListFilter shutdownServerListFilter =
            new ShutdownServerListFilter(clientManager, new StarlightClientProperties());

        List<ServiceInstance> filteredList = shutdownServerListFilter.getFilteredList(originalList);
        assertEquals(8000, filteredList.size());
        assertNotSame(filteredList, originalList);
    }
}