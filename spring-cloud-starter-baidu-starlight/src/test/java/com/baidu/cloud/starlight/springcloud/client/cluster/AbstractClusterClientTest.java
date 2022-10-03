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

import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.SpringCloudLoadbalancer;
import com.baidu.cloud.starlight.springcloud.client.properties.ClientConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/9/21.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {ApplicationContextUtils.class})
public class AbstractClusterClientTest {

    protected RibbonLoadBalancerClient loadBalancerClient;

    protected LoadBalancer loadBalancer;

    protected DiscoveryClient discoveryClient;

    protected StarlightClientProperties properties;

    protected List<ServiceInstance> serviceInstances;

    protected ServiceInstance serviceInstance;

    protected SingleStarlightClientManager clientManager;

    protected Configuration configuration;

    protected StarlightRouteProperties routeProperties;

    @Before
    public void before() throws IOException {
        properties = new StarlightClientProperties();
        ClientConfig defaultConfig = new ClientConfig();
        defaultConfig.setClusterModel(SpringCloudConstants.DEFAULT_CLUSTER_MODEL);
        defaultConfig.setCompressType("none");
        defaultConfig.setConnectTimeoutMills(1000);
        defaultConfig.setMaxHeartbeatTimes(3);
        defaultConfig.setFilters("formularequestdecorate");

        ClientConfig appConfig = new ClientConfig();
        appConfig.setClusterModel(SpringCloudConstants.DEFAULT_CLUSTER_MODEL);
        appConfig.setCompressType("none");
        appConfig.setConnectTimeoutMills(3000);
        appConfig.setIoThreadNum(1);
        appConfig.setProtocol("brpc");
        appConfig.setMaxHeartbeatTimes(365);
        appConfig.setWarmUpCount(1);
        appConfig.setWarmUpRatio(100);
        appConfig.setFilters("");

        Map<String, ClientConfig> configs = new HashMap();
        configs.put(properties.getDefaultConfig(), defaultConfig);
        configs.put("rpc-provider", appConfig);
        properties.setConfig(configs);

        serviceInstances = new LinkedList<>();
        serviceInstance = new ServiceInstance() {
            @Override
            public String getServiceId() {
                return "rpc-provider";
            }

            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public int getPort() {
                return 8899;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public java.net.URI getUri() {
                String uri = "brpc://localhost:8899";
                return java.net.URI.create(uri);
            }

            @Override
            public Map<String, String> getMetadata() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("protocols", "brpc,stargate,springrest");
                return metadata;
            }
        };
        serviceInstances.add(serviceInstance);

        loadBalancerClient = Mockito.mock(RibbonLoadBalancerClient.class);
        loadBalancer = new SpringCloudLoadbalancer(loadBalancerClient);
        // doReturn(serviceInstance).when(loadBalancerClient).choose(anyString());

        discoveryClient = Mockito.mock(DiscoveryClient.class);
        doReturn(serviceInstances).when(discoveryClient).getInstances(anyString());

        clientManager = SingleStarlightClientManager.getInstance();

        configuration = Mockito.mock(Configuration.class);
        routeProperties = new StarlightRouteProperties();

        ApplicationContext applicationContext = PowerMockito.mock(ApplicationContext.class);
        ApplicationContextUtils applicationContextUtils = new ApplicationContextUtils();
        applicationContextUtils.setApplicationContext(applicationContext);
        PowerMockito.when(applicationContext.getBean(RibbonLoadBalancerClient.class)).thenReturn(loadBalancerClient);

    }

    @Test
    public void test() {

    }
}
