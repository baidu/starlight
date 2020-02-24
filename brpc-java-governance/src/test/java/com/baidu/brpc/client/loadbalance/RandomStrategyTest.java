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

package com.baidu.brpc.client.loadbalance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.loadbalance.RandomStrategy;
import org.junit.*;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.RpcOptionsUtils;
import com.baidu.brpc.server.RpcServer;

public class RandomStrategyTest {
    private static RpcServer rpcServer1;
    private static RpcServer rpcServer2;
    private static RpcServer rpcServer3;

    private static CommunicationClient instance1;
    private static CommunicationClient instance2;
    private static CommunicationClient instance3;

    @Before
    public void before() {
        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new LoadBalanceTest.TestInterceptor(1));
        rpcServer1 = new RpcServer(8000, RpcOptionsUtils.getRpcServerOptions(), interceptors);
        rpcServer1.registerService(new LoadBalanceTest.TestEchoService(100));
        rpcServer1.start();

        interceptors = new ArrayList<Interceptor>();
        interceptors.add(new LoadBalanceTest.TestInterceptor(2));
        rpcServer2 = new RpcServer(8001, RpcOptionsUtils.getRpcServerOptions(), interceptors);
        rpcServer2.registerService(new LoadBalanceTest.TestEchoService(200));
        rpcServer2.start();

        interceptors = new ArrayList<Interceptor>();
        interceptors.add(new LoadBalanceTest.TestInterceptor(3));
        rpcServer3 = new RpcServer(8002, RpcOptionsUtils.getRpcServerOptions(), interceptors);
        rpcServer3.registerService(new LoadBalanceTest.TestEchoService(300));
        rpcServer3.start();

        ServiceInstance serviceInstance1 = new ServiceInstance("127.0.0.1", 8000);
        serviceInstance1.setServiceName("EchoService");
        instance1 = new CommunicationClient(serviceInstance1,
                RpcOptionsUtils.getCommunicationOptions(), null);

        ServiceInstance serviceInstance2 = new ServiceInstance("127.0.0.1", 8001);
        serviceInstance2.setServiceName("EchoService");
        instance2 = new CommunicationClient(serviceInstance2,
                RpcOptionsUtils.getCommunicationOptions(), null);

        ServiceInstance serviceInstance3 = new ServiceInstance("127.0.0.1", 8002);
        serviceInstance3.setServiceName("EchoService");
        instance3 = new CommunicationClient(serviceInstance3,
                RpcOptionsUtils.getCommunicationOptions(), null);
    }

    @After
    public void after() {
        if (instance1 != null) {
            instance1.stop();
        }
        if (instance2 != null) {
            instance2.stop();
        }
        if (instance3 != null) {
            instance3.stop();
        }
        if (rpcServer1 != null) {
            rpcServer1.shutdown();
        }
        if (rpcServer2 != null) {
            rpcServer2.shutdown();
        }
        if (rpcServer3 != null) {
            rpcServer3.shutdown();
        }
    }

    @Test
    public void testSelectInstance() {
        CopyOnWriteArrayList<CommunicationClient> instances = new CopyOnWriteArrayList<CommunicationClient>();
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);
        RandomStrategy randomStrategy = new RandomStrategy();
        CommunicationClient instance = randomStrategy.selectInstance(null, instances, null);
        Assert.assertTrue(instance != null);

        Set<CommunicationClient> selectedInstances = new HashSet<CommunicationClient>();
        selectedInstances.add(instance3);
        instance = randomStrategy.selectInstance(null, instances, selectedInstances);
        Assert.assertTrue(instance.getServiceInstance().getPort() != instance3.getServiceInstance().getPort());

        selectedInstances.add(instance2);
        instance = randomStrategy.selectInstance(null, instances, selectedInstances);
        Assert.assertTrue(instance.getServiceInstance().getPort() == instance1.getServiceInstance().getPort());

        selectedInstances.add(instance1);
        instance = randomStrategy.selectInstance(null, instances, selectedInstances);
        Assert.assertTrue(instance != null);
    }
}
