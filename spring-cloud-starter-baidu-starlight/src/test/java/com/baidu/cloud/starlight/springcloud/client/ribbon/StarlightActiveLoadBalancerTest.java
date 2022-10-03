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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.ClientConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.ribbon.StarlightActiveLoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.ribbon.StarlightRibbonServer;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.NoOpPing;
import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerListSubsetFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by liuruisen on 2020/12/7.
 */
public class StarlightActiveLoadBalancerTest {

    private static final String HOST = "127.0.0.1";

    @Before
    public void before() {
        StarlightClientProperties clientProperties = new StarlightClientProperties();
        ApplicationContext applicationContext = PowerMockito.mock(ApplicationContext.class);
        ApplicationContextUtils applicationContextUtils = new ApplicationContextUtils();
        applicationContextUtils.setApplicationContext(applicationContext);
        when(applicationContext.getBean(StarlightClientProperties.class)).thenReturn(clientProperties);
    }

    @Test
    public void activeServers() throws NoSuchFieldException, IllegalAccessException {

        SingleStarlightClientManager clientManager = SingleStarlightClientManager.getInstance();

        Map<String, SingleStarlightClient> starlightClientMap = new HashMap<>();

        Field field = clientManager.getClass().getDeclaredField("starlightClients");
        field.setAccessible(true);
        field.set(clientManager, starlightClientMap);

        // inactive
        SingleStarlightClient inactiveClient = Mockito.mock(SingleStarlightClient.class);
        doReturn(false).when(inactiveClient).isActive();
        starlightClientMap.put(HOST + ":" + 2234, inactiveClient);

        // active
        SingleStarlightClient activeClient = Mockito.mock(SingleStarlightClient.class);
        doReturn(true).when(activeClient).isActive();
        starlightClientMap.put(HOST + ":" + 2235, activeClient);

        DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
        clientConfig.setClientName("testApp");

        StarlightClientProperties clientProperties = new StarlightClientProperties();
        Map<String, ClientConfig> configMap = new HashMap<>();
        ClientConfig clientConfig1 = new ClientConfig();
        clientConfig1.setLocalCacheEnabled(false);
        configMap.put(clientProperties.getDefaultConfig(), clientConfig1);
        clientProperties.setConfig(configMap);

        StarlightActiveLoadBalancer activeLoadBalancer =
            new StarlightActiveLoadBalancer(clientManager, clientConfig, new RandomRule(), new NoOpPing(),
                new StaticServerList(), new ServerListSubsetFilter(), new PollingServerListUpdater(), clientProperties);

        List<Server> originalServers = new LinkedList<>();

        List<Server> activeServers = activeLoadBalancer.activeServers(originalServers);
        assertEquals(0, activeServers.size());

        StarlightRibbonServer starlightRibbonServer = new StarlightRibbonServer(HOST, 2234);
        starlightRibbonServer.setMetadata(Collections.singletonMap(SpringCloudConstants.EPOCH_KEY, 123456L - 10L + ""));
        originalServers.add(starlightRibbonServer);

        StarlightRibbonServer starlightRibbonServer2 = new StarlightRibbonServer(HOST, 2235);
        starlightRibbonServer2
            .setMetadata(Collections.singletonMap(SpringCloudConstants.EPOCH_KEY, 123456L + 10L + ""));
        originalServers.add(starlightRibbonServer2);

        List<Server> activeServers2 = activeLoadBalancer.activeServers(originalServers);
        assertEquals(2, activeServers2.size());

    }

}