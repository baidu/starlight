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
 
package com.baidu.cloud.starlight.springcloud.common;

import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by liuruisen on 2020/3/26.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationContextUtilsTest {
    private Environment environment;

    private ApplicationContext applicationContext;

    @Before
    public void before() {
        environment = mock(Environment.class);
        applicationContext = mock(ApplicationContext.class);
        ApplicationContextUtils applicationContextUtils = new ApplicationContextUtils();
        applicationContextUtils.setApplicationContext(applicationContext);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(applicationContext.getBean(String.class)).thenReturn("123");
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("none");
    }

    @Test
    public void getBeanByType() {
        assertEquals(ApplicationContextUtils.getBeanByType(String.class), "123");

        assertEquals(ApplicationContextUtils.getBeanByType(null), null);
    }

    @Test
    public void getApplicationContext() {
        assertEquals(ApplicationContextUtils.getApplicationContext(), applicationContext);
    }

    @Test
    public void getEnvironment() {
        assertEquals(ApplicationContextUtils.getEnvironment(), environment);
    }

    @Test
    public void getServerPortFromServerPort() {
        StarlightServerProperties properties = new StarlightServerProperties();
        when(applicationContext.getBean(StarlightServerProperties.class)).thenReturn(properties);
        when(environment.getProperty(SpringCloudConstants.SERVER_PORT_KEY, "8080")).thenReturn("8888");
        assertEquals(Integer.valueOf(8888), ApplicationContextUtils.getServerPort());
    }

    @Test
    public void getServerPortFromStarlightProperties() {
        StarlightServerProperties properties = new StarlightServerProperties();
        properties.setPort(8866);
        when(applicationContext.getBean(StarlightServerProperties.class)).thenReturn(properties);
        when(environment.getProperty(SpringCloudConstants.SERVER_PORT_KEY, "8888")).thenReturn("8888");
        assertEquals(Integer.valueOf(8866), ApplicationContextUtils.getServerPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getServerPortError() {
        StarlightServerProperties properties = new StarlightServerProperties();
        when(applicationContext.getBean(StarlightServerProperties.class)).thenReturn(properties);
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("servlet");
        ApplicationContextUtils.getServerPort();
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void getAppNameFromStarlightName() {

        when(environment.getProperty(SpringCloudConstants.STARLIGHT_SERVER_NAME_KEY)).thenReturn("starlight-name");

        assertEquals("starlight-name", ApplicationContextUtils.getApplicationName());
    }

    @Test
    public void getAppNameFromEmApp() {
        environmentVariables.set(SpringCloudConstants.EM_APP, "em-name");
        environmentVariables.set(SpringCloudConstants.EM_PLATFORM, "em-platform");
        environmentVariables.set(SpringCloudConstants.EM_PRODUCT_LINE, "em-product");

        doReturn(null).when(environment).getProperty(SpringCloudConstants.STARLIGHT_SERVER_NAME_KEY);

        assertEquals("em-name", ApplicationContextUtils.getApplicationName());
    }

    @Test
    public void getAppNameFromSpringName() {
        environmentVariables.set(SpringCloudConstants.EM_APP, "em-name");

        doReturn(null).when(environment).getProperty(SpringCloudConstants.STARLIGHT_SERVER_NAME_KEY);

        doReturn("spring-name").when(environment).getProperty(SpringCloudConstants.SPRING_APPLICATION_NAME_KEY);

        assertEquals("spring-name", ApplicationContextUtils.getApplicationName());

    }

    @Test
    public void getAppNameFromDefault() {
        environmentVariables.set(SpringCloudConstants.EM_APP, null);

        doReturn(null).when(environment).getProperty(SpringCloudConstants.STARLIGHT_SERVER_NAME_KEY);

        doReturn(null).when(environment).getProperty(SpringCloudConstants.SPRING_APPLICATION_NAME_KEY);

        assertEquals("application", ApplicationContextUtils.getApplicationName());
    }
}