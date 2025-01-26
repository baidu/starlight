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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/12/10.
 */
public class ClientConfigTest {

    @Test
    public void getRequestTimeoutMills() {
        ClientConfig clientConfig = new ClientConfig();
        assertNull(clientConfig.getRequestTimeoutMills(ClientConfigTest.class.getName()));

        clientConfig.setRequestTimeoutMills(100);

        assertEquals(new Integer(100), clientConfig.getRequestTimeoutMills(ClientConfigTest.class.getName()));

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setRequestTimeoutMills(200);
        interfaceConfigMap.put(ClientConfigTest.class.getName(), interfaceConfig);
        clientConfig.setInterfaceConfig(interfaceConfigMap);

        assertEquals(new Integer(200), clientConfig.getRequestTimeoutMills(ClientConfigTest.class.getName()));
    }

    @Test
    public void getRetryTimes() {
        ClientConfig clientConfig = new ClientConfig();
        assertNull(clientConfig.getRetryTimes(ClientConfigTest.class.getName()));

        clientConfig.setRetryTimes(1);

        assertEquals(new Integer(1), clientConfig.getRetryTimes(ClientConfigTest.class.getName()));

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setRetryTimes(2);
        interfaceConfigMap.put(ClientConfigTest.class.getName(), interfaceConfig);
        clientConfig.setInterfaceConfig(interfaceConfigMap);

        assertEquals(new Integer(2), clientConfig.getRetryTimes(ClientConfigTest.class.getName()));
    }

    @Test
    public void getRetryMethods() {

        ClientConfig clientConfig = new ClientConfig();
        assertNull(clientConfig.getRetryMethods(ClientConfigTest.class.getName()));

        clientConfig.setRetryMethods("retry");

        assertEquals("retry", clientConfig.getRetryMethods(ClientConfigTest.class.getName()));

        Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setRetryMethods("test");
        interfaceConfigMap.put(ClientConfigTest.class.getName(), interfaceConfig);
        clientConfig.setInterfaceConfig(interfaceConfigMap);

        assertEquals("test", clientConfig.getRetryMethods(ClientConfigTest.class.getName()));
    }

}