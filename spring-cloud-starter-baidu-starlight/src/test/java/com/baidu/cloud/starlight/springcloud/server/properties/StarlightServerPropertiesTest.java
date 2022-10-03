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
 
package com.baidu.cloud.starlight.springcloud.server.properties;

import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/25.
 */
public class StarlightServerPropertiesTest {

    private StarlightServerProperties properties;

    @Before
    public void before() {
        properties = new StarlightServerProperties();
        properties.setProtocols("brpc");
        properties.setAcceptThreadNum(1);
        properties.setAllIdleTimeout(100);
        properties.setCompressType("none");
        properties.setFilters("");
        properties.setHost("localhost");
        properties.setIoThreadNum(8);
        properties.setName("rpc-provider");
        properties.setPort(8005);
        properties.setWriteTimeoutMills(3000);
    }

    @Test
    public void serverProperties() {
        Assert.assertEquals(properties.getProtocols(), "brpc");
        Assert.assertEquals(properties.getAcceptThreadNum(), Integer.valueOf(1));
        Assert.assertEquals(properties.getAllIdleTimeout(), Integer.valueOf(100));
        Assert.assertEquals(properties.getCompressType(), "none");
        Assert.assertEquals(properties.getFilters(), "");
        Assert.assertEquals(properties.getHost(), "localhost");
        Assert.assertEquals(properties.getIoThreadNum(), Integer.valueOf(8));
        Assert.assertEquals(properties.getName(), "rpc-provider");
        Assert.assertEquals(properties.getPort(), Integer.valueOf(8005));
        Assert.assertEquals(properties.getWriteTimeoutMills(), Integer.valueOf(3000));
    }

    @Test
    public void transportConfig() {
        TransportConfig transportConfig = properties.transportConfig();
        Assert.assertEquals(transportConfig.getIoThreadNum(), Integer.valueOf(8));
        Assert.assertEquals(transportConfig.getWriteTimeoutMills(), Integer.valueOf(3000));
        Assert.assertEquals(transportConfig.getAllIdleTimeout(), Integer.valueOf(100));
        Assert.assertEquals(transportConfig.getAcceptThreadNum(), Integer.valueOf(1));
    }
}