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
 
package com.baidu.cloud.starlight.springcloud.server.register;

import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterListener;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/12/7.
 */
public class StarlightRegisterListenerTest {

    @Test
    public void starlightMetas() {

        StarlightRegisterListener registerListener = new StarlightRegisterListener() {
            @Override
            protected Registration createStarlightRegistration() {
                return null;
            }
        };

        StarlightServerProperties properties = new StarlightServerProperties();
        properties.setPort(8006);
        properties.setProtocols("brpc");

        registerListener.applicationContext = Mockito.mock(ApplicationContext.class);
        doReturn(properties).when(registerListener.applicationContext).getBean(StarlightServerProperties.class);

        Map<String, String> starlightMetas = registerListener.starlightMetas();

        assertEquals(3, starlightMetas.size());
        assertNotNull(starlightMetas.get(SpringCloudConstants.EPOCH_KEY));
        assertNotNull(starlightMetas.get(SpringCloudConstants.INTERFACES_KEY));
        assertNotNull(starlightMetas.get(SpringCloudConstants.INTERFACES_KEY));
    }
}