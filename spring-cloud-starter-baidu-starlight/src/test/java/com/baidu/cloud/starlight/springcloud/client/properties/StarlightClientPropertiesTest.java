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
 
package com.baidu.cloud.starlight.springcloud.client.properties;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.ClientConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.InterfaceConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.OutlierConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by liuruisen on 2020/3/25.
 */
public class StarlightClientPropertiesTest {

    private StarlightClientProperties clientProperties;
    private ClientConfig defaultConfig;
    private ClientConfig appConfig;
    private Map<String, ClientConfig> clientConfigMap;

    @Before
    public void before() {
        clientProperties = new StarlightClientProperties();
        defaultConfig = new ClientConfig();
        defaultConfig.setClusterModel(SpringCloudConstants.DEFAULT_CLUSTER_MODEL);
        defaultConfig.setCompressType("none");
        defaultConfig.setConnectTimeoutMills(1000);
        defaultConfig.setMaxHeartbeatTimes(3);
        defaultConfig.setFilters("formularequestdecorate");

        appConfig = new ClientConfig();
        appConfig.setClusterModel(SpringCloudConstants.DEFAULT_CLUSTER_MODEL);
        appConfig.setCompressType("none");
        appConfig.setConnectTimeoutMills(3000);
        appConfig.setIoThreadNum(1);
        appConfig.setProtocol("brpc");
        appConfig.setMaxHeartbeatTimes(365);
        appConfig.setWarmUpCount(1);
        appConfig.setWarmUpRatio(100);
        appConfig.setFilters("");
        appConfig.setWriteTimeoutMills(1000);
        appConfig.setReadIdleTimeout(1000);

        clientConfigMap = new HashMap();

        clientProperties.setConfig(clientConfigMap);
    }

    @Test
    public void getConfig() {
        Assert.assertEquals(clientProperties.getConfig().size(), 0);
    }

    @Test
    public void getClientConfig() {
        clientConfigMap.put(clientProperties.getDefaultConfig(), defaultConfig);
        ClientConfig defaultConfig = clientProperties.getClientConfig(clientProperties.getDefaultConfig());
        Assert.assertNull(defaultConfig.getProtocol());
        clientConfigMap.clear();
    }

    @Test
    public void getDefaultConfig() {
        Assert.assertEquals(clientProperties.getDefaultConfig(), "default");
    }

    @Test
    public void transportConfig() {
        // default config is null
        clientConfigMap.clear();
        clientConfigMap.put("rpc-provider", appConfig);
        TransportConfig transportConfig = clientProperties.transportConfig("rpc-provider");
        Assert.assertTrue(transportConfig.getMaxHeartbeatTimes() == 365);

        // app config is null
        clientConfigMap.clear();
        clientConfigMap.put(clientProperties.getDefaultConfig(), defaultConfig);
        TransportConfig transportConfig2 = clientProperties.transportConfig("rpc-provider");
        Assert.assertTrue(transportConfig2.getMaxHeartbeatTimes() == 3);

        // app config value is null
        clientConfigMap.clear();
        clientConfigMap.put(clientProperties.getDefaultConfig(), defaultConfig);
        appConfig.setReadIdleTimeout(null);
        appConfig.setWriteTimeoutMills(null);
        appConfig.setMaxHeartbeatTimes(null);
        appConfig.setConnectTimeoutMills(null);
        appConfig.setCompressType(null);
        clientConfigMap.put("rpc-provider", appConfig);
        TransportConfig transportConfig3 = clientProperties.transportConfig("rpc-provider");
        Assert.assertTrue(transportConfig3.getMaxHeartbeatTimes() == 3);
    }

    @Test
    public void getIoThreadNum() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.DEFAULT_IO_THREADS_VALUE, properties.getIoThreadNum("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setIoThreadNum(999);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(999, properties.getIoThreadNum("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setIoThreadNum(888);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(888, properties.getIoThreadNum("Test").intValue());
    }

    @Test
    public void getConnectTimeoutMills() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.CONNECT_TIMEOUT_VALUE.intValue(), properties.getConnectTimeoutMills("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setConnectTimeoutMills(999);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(999, properties.getConnectTimeoutMills("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setConnectTimeoutMills(888);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(888, properties.getConnectTimeoutMills("Test").intValue());
    }

    @Test
    public void getMaxHeartbeatTimes() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.MAX_HEARTBEAT_TIMES_VALUE, properties.getMaxHeartbeatTimes("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setMaxHeartbeatTimes(999);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(999, properties.getMaxHeartbeatTimes("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setMaxHeartbeatTimes(888);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(888, properties.getMaxHeartbeatTimes("Test").intValue());
    }

    @Test
    public void getWriteTimeoutMills() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.WRITE_TIMEOUT_VALUE.intValue(), properties.getWriteTimeoutMills("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setWriteTimeoutMills(999);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(999, properties.getWriteTimeoutMills("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWriteTimeoutMills(888);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(888, properties.getWriteTimeoutMills("Test").intValue());
    }

    @Test
    public void getReadIdleTimeout() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.READ_IDLE_TIMEOUT_VALUE, properties.getReadIdleTimeout("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setReadIdleTimeout(999);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(999, properties.getReadIdleTimeout("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setReadIdleTimeout(888);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(888, properties.getReadIdleTimeout("Test").intValue());

    }

    @Test
    public void getRequestTimeoutMills() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.REQUEST_TIMEOUT_VALUE.intValue(), properties.getRequestTimeoutMills("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setRequestTimeoutMills(999);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(999, properties.getRequestTimeoutMills("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRequestTimeoutMills(888);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(888, properties.getRequestTimeoutMills("Test").intValue());

    }

    @Test
    public void getWarmUpRatio() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(SpringCloudConstants.DEFAULT_WARM_UP_RATIO.intValue(),
            properties.getWarmUpRatio("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setWarmUpRatio(99);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(99, properties.getWarmUpRatio("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWarmUpRatio(88);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(88, properties.getWarmUpRatio("Test").intValue());

    }

    @Test
    public void getWarmUpCount() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertNull(properties.getWarmUpCount("Test"));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setWarmUpCount(99);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(99, properties.getWarmUpCount("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWarmUpCount(88);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(88, properties.getWarmUpCount("Test").intValue());

    }

    @Test
    public void getChannelType() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.DEFAULT_RPC_CHANNEL_TYPE_VALUE, properties.getChannelType("Test"));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setChannelType("123");
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals("123", properties.getChannelType("Test"));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setChannelType("456");
        clientConfigMap.put("Test", clientConfig);

        assertEquals("456", properties.getChannelType("Test"));

    }

    @Test
    public void getMaxConnections() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.MAX_TOTAL_CONNECTIONS.intValue(), properties.getMaxConnections("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setMaxConnections(99);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(99, properties.getMaxConnections("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setMaxConnections(88);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(88, properties.getMaxConnections("Test").intValue());
    }

    @Test
    public void getMaxIdleConnections() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.MAX_IDLE_CONNECTIONS.intValue(), properties.getMaxIdleConnections("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setMaxIdleConnections(99);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(99, properties.getMaxIdleConnections("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setMaxIdleConnections(88);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(88, properties.getMaxIdleConnections("Test").intValue());
    }

    @Test
    public void getMinIdleConnections() {

        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.MIN_IDLE_CONNECTIONS.intValue(), properties.getMinIdleConnections("Test").intValue());

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setMinIdleConnections(99);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(99, properties.getMinIdleConnections("Test").intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setMinIdleConnections(88);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(88, properties.getMinIdleConnections("Test").intValue());
    }

    @Test
    public void getProtocol() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.BRPC_VALUE, properties.getProtocol("Test"));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setProtocol("brpc");
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals("brpc", properties.getProtocol("Test"));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setProtocol("stargate");
        clientConfigMap.put("Test", clientConfig);

        assertEquals("stargate", properties.getProtocol("Test"));

    }

    @Test
    public void getCompressType() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals("none", properties.getCompressType("Test"));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setCompressType("brpc");
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals("brpc", properties.getCompressType("Test"));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setCompressType("stargate");
        clientConfigMap.put("Test", clientConfig);

        assertEquals("stargate", properties.getCompressType("Test"));

    }

    @Test
    public void getFilters() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(SpringCloudConstants.DEFAULT_CLIENT_FILTERS, properties.getFilters("Test"));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setFilters("brpc");
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(SpringCloudConstants.DEFAULT_CLIENT_FILTERS + ",brpc", properties.getFilters("Test"));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setFilters("stargate");
        clientConfigMap.put("Test", clientConfig);

        assertEquals(SpringCloudConstants.DEFAULT_CLIENT_FILTERS + ",stargate", properties.getFilters("Test"));
    }

    @Test
    public void getRequestTimeoutMills2() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(Constants.REQUEST_TIMEOUT_VALUE,
            properties.getRequestTimeoutMills("Test", this.getClass().getName()));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setRequestTimeoutMills(111);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(111, properties.getRequestTimeoutMills("Test", this.getClass().getName()).intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRequestTimeoutMills(222);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(222, properties.getRequestTimeoutMills("Test", this.getClass().getName()).intValue());

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        defalutConfig.setInterfaceConfig(interfaceConfigMap);
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfigMap.put(this.getClass().getName(), interfaceConfig);
        interfaceConfig.setRequestTimeoutMills(333);

        assertEquals(222, properties.getRequestTimeoutMills("Test", this.getClass().getName()).intValue());

        Map<String, InterfaceConfig> interfaceConfigMap1 = new HashMap<>();
        clientConfig.setInterfaceConfig(interfaceConfigMap1);
        InterfaceConfig config = new InterfaceConfig();
        interfaceConfigMap1.put(this.getClass().getName(), config);
        config.setRequestTimeoutMills(444);
        assertEquals(444, properties.getRequestTimeoutMills("Test", this.getClass().getName()).intValue());
    }

    @Test
    public void getRetryTimes() {
        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(SpringCloudConstants.DEFAULT_RETRY_TIMES,
            properties.getRetryTimes("Test", this.getClass().getName()));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setRetryTimes(111);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(111, properties.getRetryTimes("Test", this.getClass().getName()).intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRetryTimes(222);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(222, properties.getRetryTimes("Test", this.getClass().getName()).intValue());

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        defalutConfig.setInterfaceConfig(interfaceConfigMap);
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfigMap.put(this.getClass().getName(), interfaceConfig);
        interfaceConfig.setRetryTimes(333);

        assertEquals(222, properties.getRetryTimes("Test", this.getClass().getName()).intValue());

        Map<String, InterfaceConfig> interfaceConfigMap1 = new HashMap<>();
        clientConfig.setInterfaceConfig(interfaceConfigMap1);
        InterfaceConfig config = new InterfaceConfig();
        interfaceConfigMap1.put(this.getClass().getName(), config);
        config.setRetryTimes(444);
        assertEquals(444, properties.getRetryTimes("Test", this.getClass().getName()).intValue());

    }

    @Test
    public void getRetryDelayTimeUnitMills() {

        StarlightClientProperties properties = new StarlightClientProperties();
        assertEquals(SpringCloudConstants.DEFAULT_RETRY_DELAY_MILLS,
            properties.getRetryDelayTimeUnitMills("Test", this.getClass().getName()));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setRetryDelayTimeUnitMills(111);
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals(111, properties.getRetryDelayTimeUnitMills("Test", this.getClass().getName()).intValue());

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRetryDelayTimeUnitMills(222);
        clientConfigMap.put("Test", clientConfig);

        assertEquals(222, properties.getRetryDelayTimeUnitMills("Test", this.getClass().getName()).intValue());

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        defalutConfig.setInterfaceConfig(interfaceConfigMap);
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfigMap.put(this.getClass().getName(), interfaceConfig);
        interfaceConfig.setRetryDelayTimeUnitMills(333);

        assertEquals(222, properties.getRetryDelayTimeUnitMills("Test", this.getClass().getName()).intValue());

        Map<String, InterfaceConfig> interfaceConfigMap1 = new HashMap<>();
        clientConfig.setInterfaceConfig(interfaceConfigMap1);
        InterfaceConfig config = new InterfaceConfig();
        interfaceConfigMap1.put(this.getClass().getName(), config);
        config.setRetryDelayTimeUnitMills(444);
        assertEquals(444, properties.getRetryDelayTimeUnitMills("Test", this.getClass().getName()).intValue());
    }

    @Test
    public void getRetryMethods() {

        StarlightClientProperties properties = new StarlightClientProperties();
        assertNull(properties.getRetryMethods("Test", this.getClass().getName()));

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        defalutConfig.setRetryMethods("111");
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        assertEquals("111", properties.getRetryMethods("Test", this.getClass().getName()));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRetryMethods("222");
        clientConfigMap.put("Test", clientConfig);

        assertEquals("222", properties.getRetryMethods("Test", this.getClass().getName()));

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        defalutConfig.setInterfaceConfig(interfaceConfigMap);
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfigMap.put(this.getClass().getName(), interfaceConfig);
        interfaceConfig.setRetryMethods("333");

        assertEquals("222", properties.getRetryMethods("Test", this.getClass().getName()));

        Map<String, InterfaceConfig> interfaceConfigMap1 = new HashMap<>();
        clientConfig.setInterfaceConfig(interfaceConfigMap1);
        InterfaceConfig config = new InterfaceConfig();
        interfaceConfigMap1.put(this.getClass().getName(), config);
        config.setRetryMethods("444");
        assertEquals("444", properties.getRetryMethods("Test", this.getClass().getName()));

    }

    @Test
    public void outlierConifg() {
        StarlightClientProperties properties = new StarlightClientProperties();

        Map<String, ClientConfig> clientConfigMap = new HashMap<>();
        properties.setConfig(clientConfigMap);

        ClientConfig defalutConfig = new ClientConfig();
        clientConfigMap.put(properties.getDefaultConfig(), defalutConfig);

        ClientConfig appConfiggh = new ClientConfig();
        clientConfigMap.put("starlight-provider", appConfiggh);

        OutlierConfig outlierConfig = new OutlierConfig();
        outlierConfig.setEnabled(true);
        outlierConfig.setDetectInterval(60);
        outlierConfig.setBaseEjectTime(120);
        outlierConfig.setFailureCountThreshold(120);
        outlierConfig.setFailurePercentMinRequest(1);
        outlierConfig.setMaxEjectTime(600);
        defalutConfig.setOutlier(outlierConfig);
        assertEquals(true, properties.getOutlierConfig("starlight-provider").getEnabled());
        assertEquals(true, outlierConfig.getEnabled());

        OutlierConfig config = new OutlierConfig();
        config.setEnabled(false);
        config.setDetectInterval(120);
        config.setBaseEjectTime(300);
        config.setFailureCountThreshold(300);
        config.setFailurePercentMinRequest(3);
        appConfiggh.setOutlier(config);
        assertEquals(false, properties.getOutlierConfig("starlight-provider").getEnabled());
        assertEquals(true, outlierConfig.getEnabled());
        assertEquals(false, config.getEnabled());
        assertEquals(20, properties.getOutlierConfig("starlight-provider").getMaxEjectPercent().intValue());

        assertEquals(true, properties.getOutlierConfig("starlight-app").getEnabled());
        assertEquals(20, properties.getOutlierConfig("starlight-app").getMaxEjectPercent().intValue());
    }

}