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
 
package com.baidu.cloud.starlight.springcloud.client.outlier;

import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.outlier.OutlierEjectServerListFilter;
import com.baidu.cloud.starlight.springcloud.client.properties.ClientConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.OutlierConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.netflix.loadbalancer.Server;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Created by liuruisen on 2021/4/25.
 */
public class OutlierEjectServerListFilterTest {

    private SingleStarlightClientManager clientManager = SingleStarlightClientManager.getInstance();

    public List<Server> serverList() {
        List<Server> originalList = new LinkedList<>();
        for (int i = 0; i < 10000; i++) {
            Server server = new TestServer("localhost", 1000 + i);
            SingleStarlightClient singleClient =
                clientManager.getOrCreateSingleClient(server.getHost(), server.getPort(), new TransportConfig());
            if (i < 2000) { // mark server as outlier
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.OUTLIER, System.currentTimeMillis()));
            }

            if (i >= 2000 && i < 4000) {
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.SHUTTING_DOWN, System.currentTimeMillis()));
            }

            if (i >= 4000) {
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));
            }
            originalList.add(server);
        }

        return originalList;
    }

    @Test
    public void getFilteredList() {
        StarlightClientProperties clientProperties = new StarlightClientProperties();
        OutlierConfig outlierConfig = new OutlierConfig();
        outlierConfig.setMaxEjectPercent(90);
        outlierConfig.setBaseEjectTime(30);
        outlierConfig.setMaxEjectTime(300);
        outlierConfig.setEnabled(true);

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setOutlier(outlierConfig);

        clientProperties.setConfig(Collections.singletonMap("testApp", clientConfig));

        OutlierEjectServerListFilter serverListFilter =
            new OutlierEjectServerListFilter(clientManager, clientProperties, "testApp");

        List<Server> originalList = serverList();
        // eject all 2000 OUTLIER
        List<Server> filteredList = serverListFilter.getFilteredList(originalList);
        assertEquals(8000, filteredList.size());
        assertNotSame(filteredList, originalList);

        // eject 1000 OUTLIER
        outlierConfig.setMaxEjectPercent(10);
        List<Server> filterList2 = serverListFilter.getFilteredList(originalList);
        assertEquals(9000, filterList2.size());
        assertNotSame(filteredList, originalList);
    }

    @Test
    public void getFilteredListSize1() {
        StarlightClientProperties clientProperties = new StarlightClientProperties();
        OutlierConfig outlierConfig = new OutlierConfig();
        outlierConfig.setMaxEjectPercent(90);
        outlierConfig.setBaseEjectTime(30);
        outlierConfig.setMaxEjectTime(300);
        outlierConfig.setEnabled(true);

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setOutlier(outlierConfig);

        clientProperties.setConfig(Collections.singletonMap("testApp", clientConfig));

        OutlierEjectServerListFilter serverListFilter =
            new OutlierEjectServerListFilter(clientManager, clientProperties, "testApp");

        Server server = new TestServer("localhost", 20000);
        List<Server> originalList = new LinkedList<>();
        originalList.add(server);
        originalList.add(new Server("localhost", 10000));
        SingleStarlightClient singleClient =
            clientManager.getOrCreateSingleClient(server.getHost(), server.getPort(), new TransportConfig());
        singleClient.updateStatus(new PeerStatus(PeerStatus.Status.OUTLIER, System.currentTimeMillis()));

        // eject zero OUTLIER
        List<Server> filteredList = serverListFilter.getFilteredList(originalList);
        assertEquals(1, filteredList.size());
        assertNotSame(filteredList, originalList);
    }

    private class TestServer extends Server {

        public TestServer(String host, int port) {
            super(host, port);
        }

        @Override
        public MetaInfo getMetaInfo() {
            return new MetaInfo() {
                @Override
                public String getAppName() {
                    return "testApp";
                }

                @Override
                public String getServerGroup() {
                    return null;
                }

                @Override
                public String getServiceIdForDiscovery() {
                    return null;
                }

                @Override
                public String getInstanceId() {
                    return "testInstanceId";
                }
            };
        }
    }
}