/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.client;

import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.RpcMethodInfo;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Map;

public class RpcProxyTest {

    @Test
    public void testRpcProxy() {
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8002");
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        System.out.println(echoService.toString());
        System.out.println(echoService.hashCode());
    }

    @Test
    public void testConstructor() throws Exception {
        Class[] paramTypes = new Class[2];
        paramTypes[0] = RpcClient.class;
        paramTypes[1] = Class.class;
        Constructor constructor = BrpcProxy.class.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8002");
        BrpcProxy rpcProxy = (BrpcProxy) constructor.newInstance(rpcClient, EchoService.class);
        Assert.assertTrue(rpcProxy != null);
        Map<String, RpcMethodInfo> methodInfoMap = rpcProxy.getRpcMethodMap();
        Assert.assertTrue(methodInfoMap.size() > 0);
        RpcMethodInfo rpcMethodInfo = methodInfoMap.entrySet().iterator().next().getValue();
        Assert.assertTrue(rpcMethodInfo != null);
    }
}
