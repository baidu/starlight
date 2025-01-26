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
 
package com.baidu.cloud.starlight.springcloud.server.register.consul;

import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import com.ecwid.consul.v1.agent.model.NewService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/3/26.
 */
@RunWith(MockitoJUnitRunner.class)
public class StarlightConsulRegisterListenerTest {

    private StarlightConsulRegisterListener consulRegisterListener;

    private ApplicationStartedEvent applicationEvent;

    private ConsulDiscoveryProperties discoveryProperties;

    private HeartbeatProperties heartbeatProperties;

    private ConfigurableEnvironment environment;

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        consulRegisterListener = new StarlightConsulRegisterListener();

        environment = Mockito.mock(ConfigurableEnvironment.class);
        doReturn("rpc-provider").when(environment).getProperty("starlight.server.name", "starlight-server");
        doReturn("instanceId").when(environment).getProperty("spring.cloud.consul.discovery.instanceId", "instanceId");

        ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);
        doNothing().when(serviceRegistry).register(any());
        doNothing().when(serviceRegistry).deregister(any());

        discoveryProperties = Mockito.mock(ConsulDiscoveryProperties.class);
        doReturn("localhost").when(discoveryProperties).getHostname();
        doReturn("1s").when(discoveryProperties).getHealthCheckInterval();
        doReturn("1223").when(discoveryProperties).getHealthCheckTimeout();
        doReturn("2312").when(discoveryProperties).getHealthCheckCriticalTimeout();
        doReturn(true).when(discoveryProperties).getHealthCheckTlsSkipVerify();

        heartbeatProperties = Mockito.mock(HeartbeatProperties.class);
        doReturn(false).when(heartbeatProperties).isEnabled();

        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
        doReturn(serviceRegistry).when(applicationContext).getBean(ServiceRegistry.class);
        doReturn(discoveryProperties).when(applicationContext).getBean(ConsulDiscoveryProperties.class);
        doReturn(environment).when(applicationContext).getEnvironment();
        doReturn(heartbeatProperties).when(applicationContext).getBean(HeartbeatProperties.class);
        // StarlightServerProperties
        StarlightServerProperties properties = new StarlightServerProperties();
        properties.setPort(8006);
        doReturn(properties).when(applicationContext).getBean(StarlightServerProperties.class);

        applicationEvent = Mockito.mock(ApplicationStartedEvent.class);
        doReturn(applicationContext).when(applicationEvent).getApplicationContext();

        Field field = consulRegisterListener.getClass().getSuperclass().getDeclaredField("serviceRegistry");
        field.setAccessible(true);
        field.set(consulRegisterListener, serviceRegistry);

        Field field2 = consulRegisterListener.getClass().getSuperclass().getDeclaredField("applicationContext");
        field2.setAccessible(true);
        field2.set(consulRegisterListener, applicationContext);

        ApplicationContextUtils applicationContextUtils = new ApplicationContextUtils();
        applicationContextUtils.setApplicationContext(applicationContext);
    }

    @Test
    public void createStarlightRegistration() {
        Registration registration = consulRegisterListener.createStarlightRegistration();
        Assert.assertTrue(registration instanceof ConsulRegistration);

        ConsulRegistration consulRegistration = (ConsulRegistration) registration;
        Assert.assertEquals(consulRegistration.getHost(), "localhost");
        Assert.assertEquals(consulRegistration.getPort(), 8006);
        NewService service = consulRegistration.getService();
        Assert.assertNotNull(service);
    }

    @Test
    public void createStarlightRegistrationError() {
        doReturn("").when(environment).getProperty("starlight.server.name", "starlight-server");
        try {
            consulRegisterListener.createStarlightRegistration();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        doReturn("-1").when(environment).getProperty("starlight.server.port");
        try {
            consulRegisterListener.createStarlightRegistration();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        doReturn("rpc-provider").when(environment).getProperty("starlight.server.name", "starlight-server");
        doReturn("8006").when(environment).getProperty("starlight.server.port");
    }

    @Test
    public void createStarlightRegistrationFullConfig() {
        doReturn(true).when(discoveryProperties).isPreferAgentAddress();
        doReturn("localhost").when(discoveryProperties).getHostname();

        doReturn(true).when(heartbeatProperties).isEnabled();
        doReturn("3s").when(heartbeatProperties).getTtl();

        doReturn("").when(discoveryProperties).getHealthCheckCriticalTimeout();

        Registration registration = consulRegisterListener.createStarlightRegistration();
        Assert.assertTrue(registration instanceof ConsulRegistration);
        ConsulRegistration consulRegistration = (ConsulRegistration) registration;
        Assert.assertEquals(consulRegistration.getService().getAddress(), null);
        Assert.assertEquals(consulRegistration.getPort(), 8006);
        NewService service = consulRegistration.getService();
        Assert.assertNotNull(service);
    }

    @Test
    public void onApplication() {
        // consulRegisterListener.onApplicationEvent(applicationEvent);
        consulRegisterListener.deRegister();
    }

    @Test
    public void deRegister() {
        consulRegisterListener.deRegister();
    }
}