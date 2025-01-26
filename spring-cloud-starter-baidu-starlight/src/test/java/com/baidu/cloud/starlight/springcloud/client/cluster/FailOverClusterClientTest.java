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

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.core.rpc.callback.FutureCallback;
import com.baidu.cloud.starlight.springcloud.client.properties.ClientConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.InterfaceConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/9/21.
 */
public class FailOverClusterClientTest extends AbstractClusterClientTest {

    @Test
    public void retryRequest() throws IOException, NoSuchFieldException, IllegalAccessException {
        ClientConfig defaultConfig =
                properties.getClientConfig(properties.getDefaultConfig());
        defaultConfig.setFilters("");
        defaultConfig.setRetryMethods("");
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setRetryMethods("retry");
        interfaceConfig.setRetryTimes(3);
        defaultConfig.setInterfaceConfig(
                Collections.singletonMap(FailOverClusterClient.class.getName(), interfaceConfig));

        FailOverClusterClient clusterClient =
                new FailOverClusterClient("rpc-provider", properties,
                        loadBalancer, discoveryClient, clientManager, configuration, routeProperties);
        clusterClient.init();
        ServiceConfig serviceConfig = new ServiceConfig();
        clusterClient.refer(FailOverClusterClient.class, serviceConfig);

        Request request = new RpcRequest();
        request.setServiceClass(FailOverClusterClient.class);
        request.setMethodName("retry");
        request.setServiceConfig(serviceConfig);
        ResultFuture resultFuture = new ResultFuture();
        RpcCallback rpcCallback = new FutureCallback(resultFuture, request);

        doReturn(serviceInstance).when(loadBalancerClient).choose(anyString());
        doReturn(null).when(loadBalancerClient).execute(any(), any(), any());
        RpcContext.getContext().setRequestTimeoutMills(3000);
        clusterClient.request(request, rpcCallback);

        Class failOverClusterClass = clusterClient.getClass();
        Field remainedRetriesField = failOverClusterClass.getDeclaredField("remainedRetries");
        remainedRetriesField.setAccessible(true);
        Map<Long, AtomicInteger> remainedReties = (Map<Long, AtomicInteger>) remainedRetriesField.get(clusterClient);
        assertNotNull(remainedReties);
        assertEquals(1, remainedReties.size());


        Field retryTimesMapField = failOverClusterClass.getDeclaredField("retryTimesMap");
        retryTimesMapField.setAccessible(true);
        Map<Long, Integer> retryTimesMap = (Map<Long, Integer>) retryTimesMapField.get(clusterClient);
        assertNotNull(retryTimesMap);
        assertEquals(1, retryTimesMap.size());

        // TODO 如何测试retry能力
    }

}