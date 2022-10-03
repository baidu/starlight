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
 
package com.baidu.cloud.starlight.core.rpc.proxy;

import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class JDKProxyFactoryTest {

    private JDKProxyFactory jdkProxyFactory;

    private StarlightClient starlightClient;

    @Before
    public void before() {
        jdkProxyFactory = new JDKProxyFactory();
        starlightClient = Mockito.mock(StarlightClient.class);
        doNothing().when(starlightClient).request(any(), any());
    }

    @Test
    public void getProxy() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setProtocol("brpc");
        TestService testService = jdkProxyFactory.getProxy(TestService.class, serviceConfig, starlightClient);

        Assert.assertFalse(testService.equals("test"));
        Assert.assertEquals(testService.hashCode(), testService.hashCode());
        Assert.assertEquals(testService.toString(), testService.toString());
        AsyncTestService asyncTestService =
            jdkProxyFactory.getProxy(AsyncTestService.class, serviceConfig, starlightClient);
        asyncTestService.echoCallback("Hello", new Callback() {
            @Override
            public void onResponse(Object response) {}

            @Override
            public void onError(Throwable e) {}
        });
        try {
            asyncTestService.testCallback("hello", new Callback() {
                @Override
                public void onResponse(Object response) {}

                @Override
                public void onError(Throwable e) {}
            });
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
        }
    }
}