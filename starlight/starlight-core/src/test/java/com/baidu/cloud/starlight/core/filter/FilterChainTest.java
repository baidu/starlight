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
 
package com.baidu.cloud.starlight.core.filter;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.core.rpc.RpcClientInvoker;
import com.baidu.cloud.starlight.core.rpc.RpcServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.core.rpc.callback.FutureCallback;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class FilterChainTest {

    @Test
    public void buildClientChainInvoker() {
        ClientInvoker clientInvoker = Mockito.mock(RpcClientInvoker.class);
        doNothing().when(clientInvoker).invoke(any(), any());
        clientInvoker = FilterChain.buildClientChainInvoker(clientInvoker, "test,testsame");

        Request request = new RpcRequest();
        clientInvoker.invoke(request, new FutureCallback(null, request));
    }

    @Test
    public void buildServerChainInvoker() {
        ServiceInvoker serviceInvoker = Mockito.mock(RpcServiceInvoker.class);
        doNothing().when(serviceInvoker).invoke(any(), any());
        serviceInvoker = FilterChain.buildServerChainInvoker(serviceInvoker, "test,testsame");
        Request request = new RpcRequest();
        serviceInvoker.invoke(request, new FutureCallback(null, request));
    }

    @Test
    public void buildServerChainIllegal() {
        ServiceInvoker serviceInvoker = Mockito.mock(RpcServiceInvoker.class);
        doNothing().when(serviceInvoker).invoke(any(), any());
        serviceInvoker = FilterChain.buildServerChainInvoker(serviceInvoker, "");
        Request request = new RpcRequest();
        serviceInvoker.invoke(request, new FutureCallback(null, request));

        // blank filter
        serviceInvoker = FilterChain.buildServerChainInvoker(serviceInvoker, "  , ");

        // null filter
        serviceInvoker = FilterChain.buildServerChainInvoker(serviceInvoker, null);

        // unSupport filter
        try {
            serviceInvoker = FilterChain.buildServerChainInvoker(serviceInvoker, "asd");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }

}