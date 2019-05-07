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
package com.baidu.brpc.naming.zookeeper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.naming.SubscribeInfo;

public class ZookeeperNamingServiceTest {
    private TestingServer zkServer;
    private BrpcURL namingUrl;
    private ZookeeperNamingService namingService;

    public void setUp() throws Exception {
        zkServer = new TestingServer(2087, true);
        namingUrl = new BrpcURL("zookeeper://127.0.0.1:2087");
        namingService = new ZookeeperNamingService(namingUrl);
    }

    public void tearDown() throws Exception {
        zkServer.stop();
    }

    protected RegisterInfo createRegisterInfo(String host, int port) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setHost(host);
        registerInfo.setPort(port);
        registerInfo.setService(EchoService.class.getName());
        return registerInfo;
    }

    protected SubscribeInfo createSubscribeInfo(boolean ignoreFail) {
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setService(EchoService.class.getName());
        subscribeInfo.setIgnoreFailOfNamingService(ignoreFail);
        return subscribeInfo;
    }

    @Test
    public void testLookup() throws Exception {
        setUp();
        SubscribeInfo subscribeInfo = createSubscribeInfo(true);
        List<ServiceInstance> instances = namingService.lookup(subscribeInfo);
        Assert.assertTrue(instances.size() == 0);

        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8012);
        namingService.register(registerInfo);
        instances = namingService.lookup(subscribeInfo);
        Assert.assertTrue(instances.size() == 1);
        Assert.assertTrue(instances.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(instances.get(0).getPort() == 8012);
        namingService.unregister(registerInfo);
        tearDown();
    }

    @Test
    public void testSubscribe() throws Exception {
        setUp();
        final List<ServiceInstance> adds = new ArrayList<ServiceInstance>();
        final List<ServiceInstance> deletes = new ArrayList<ServiceInstance>();
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        namingService.subscribe(subscribeInfo, new NotifyListener() {
            @Override
            public void notify(Collection<ServiceInstance> addList, Collection<ServiceInstance> deleteList) {
                System.out.println("receive new subscribe info time:" + System.currentTimeMillis());
                System.out.println("add size:" + addList.size());
                for (ServiceInstance instance : addList) {
                    System.out.println(instance);
                }
                adds.addAll(addList);

                System.out.println("delete size:" + deleteList.size());
                for (ServiceInstance instance : deleteList) {
                    System.out.println(instance);
                }
                deletes.addAll(deleteList);
            }
        });
        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8013);
        namingService.register(registerInfo);
        System.out.println("register time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 1);
        Assert.assertTrue(deletes.size() == 0);
        Assert.assertTrue(adds.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(adds.get(0).getPort() == 8013);
        adds.clear();
        deletes.clear();

        namingService.unregister(registerInfo);
        System.out.println("unregister time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 0);
        Assert.assertTrue(deletes.size() == 1);
        Assert.assertTrue(deletes.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(deletes.get(0).getPort() == 8013);

        namingService.unsubscribe(subscribeInfo);
        tearDown();
    }

    /**
     * This test must test under actual zookeeper server, Not the TestingServer of Curator
     */
    @Test
    @Ignore
    public void testSubscribeWhenZookeeperDownAndUp() throws Exception {
        namingUrl = new BrpcURL("zookeeper://127.0.0.1:2181");
        namingService = new ZookeeperNamingService(namingUrl);

        final List<ServiceInstance> adds = new ArrayList<ServiceInstance>();
        final List<ServiceInstance> deletes = new ArrayList<ServiceInstance>();
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        namingService.subscribe(subscribeInfo, new NotifyListener() {
            @Override
            public void notify(Collection<ServiceInstance> addList, Collection<ServiceInstance> deleteList) {
                System.out.println("receive new subscribe info time:" + System.currentTimeMillis());
                System.out.println("add size:" + addList.size());
                for (ServiceInstance instance : addList) {
                    System.out.println(instance);
                }
                adds.addAll(addList);

                System.out.println("delete size:" + deleteList.size());
                for (ServiceInstance instance : deleteList) {
                    System.out.println(instance);
                }
                deletes.addAll(deleteList);
            }
        });
        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8014);
        namingService.register(registerInfo);
        System.out.println("register time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 1);
        Assert.assertTrue(deletes.size() == 0);
        Assert.assertTrue(adds.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(adds.get(0).getPort() == 8014);
        adds.clear();
        deletes.clear();

        // sleep for restarting zookeeper
        Thread.sleep(30 * 1000);

        List<ServiceInstance> instances = namingService.lookup(subscribeInfo);
        Assert.assertTrue(instances.size() == 1);
        Assert.assertTrue(instances.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(instances.get(0).getPort() == 8014);

        namingService.unregister(registerInfo);
        System.out.println("unregister time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 0);
        Assert.assertTrue(deletes.size() == 1);
        Assert.assertTrue(deletes.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(deletes.get(0).getPort() == 8014);

        namingService.unsubscribe(subscribeInfo);
    }

    /**
     * This test must test under actual zookeeper server, Not the TestingServer of Curator
     */
    @Test
    @Ignore
    public void testRegisterWhenZookeeperDownAndUp() throws Exception {
        namingUrl = new BrpcURL("zookeeper://127.0.0.1:2181");
        namingService = new ZookeeperNamingService(namingUrl);

        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8015);
        namingService.register(registerInfo);
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        List<ServiceInstance> instances = namingService.lookup(subscribeInfo);
        Assert.assertTrue(instances.size() == 1);
        Assert.assertTrue(instances.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(instances.get(0).getPort() == 8015);

        // sleep for restarting zookeeper
        Thread.sleep(30 * 1000);
        instances = namingService.lookup(subscribeInfo);
        Assert.assertTrue(instances.size() == 1);
        System.out.println(instances.get(0));
        Assert.assertTrue(instances.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(instances.get(0).getPort() == 8015);
        namingService.unregister(registerInfo);
    }
}
