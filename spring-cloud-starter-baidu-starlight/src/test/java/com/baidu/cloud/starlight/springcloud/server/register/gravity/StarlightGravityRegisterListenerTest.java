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
 
package com.baidu.cloud.starlight.springcloud.server.register.gravity;

import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/8/11.
 */
public class StarlightGravityRegisterListenerTest {

    private ConfigurableEnvironment environment;

    private StarlightGravityRegisterListener listener;

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        listener = new StarlightGravityRegisterListener();

        // mock environment
        environment = Mockito.mock(ConfigurableEnvironment.class);
        doReturn("rpc-provider").when(environment).getProperty("starlight.server.name");
        doReturn("instanceId").when(environment).getProperty("spring.cloud.consul.discovery.instanceId", "instanceId");
        doReturn("brpc,stargate,springrest").when(environment).getProperty("starlight.server.protocols");

        // StarlightServerProperties
        StarlightServerProperties properties = new StarlightServerProperties();
        properties.setPort(8006);

        // mock applicationContext
        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
        doReturn(environment).when(applicationContext).getEnvironment();
        doReturn(properties).when(applicationContext).getBean(StarlightServerProperties.class);

        // set applicationContext
        Field field2 = listener.getClass().getSuperclass().getDeclaredField("applicationContext");
        field2.setAccessible(true);
        field2.set(listener, applicationContext);

        ApplicationContextUtils applicationContextUtils = new ApplicationContextUtils();
        applicationContextUtils.setApplicationContext(applicationContext);
    }

    @Test
    public void createStarlightRegistration() {
        GravityRegistration registration = (GravityRegistration) listener.createStarlightRegistration();
        Assert.assertNotNull(registration.getServiceId());
        assertEquals("rpc-provider", registration.getServiceId()); // service
        assertEquals("rpc", registration.getScheme()); // schema
        assertEquals(8006, registration.getPort()); // port
        assertEquals("brpc,stargate,springrest", registration.getMetadata().get("protocols"));
    }
}