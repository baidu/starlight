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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class RpcClientInvokerTest {

    private RpcClientInvoker clientInvoker;

    @Before
    public void before() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setServiceId(UserService.class.getName());
        serviceConfig.setProtocol("brpc");

        ClientPeer clientPeer = Mockito.mock(ClientPeer.class);
        doNothing().when(clientPeer).request(any(), any());
        clientInvoker = new RpcClientInvoker(clientPeer, serviceConfig);
    }

    @Test
    public void getClientPeer() {
        Assert.assertNotNull(clientInvoker.getClientPeer());
    }

    @Test
    public void invoke() {
        Request request = new RpcRequest();
        request.setMethodName("getUser");
        request.setServiceClass(UserService.class);
        request.setParams(new Object[] {1l});
        request.setParamsTypes(new Class[] {Long.class});
        request.setProtocolName("brpc");
        request.setReturnType(User.class);
        clientInvoker.invoke(request, null);
    }

    @Test
    public void destroy() {
        clientInvoker.destroy();
    }

    @Test
    public void init() {
        clientInvoker.init();
    }
}