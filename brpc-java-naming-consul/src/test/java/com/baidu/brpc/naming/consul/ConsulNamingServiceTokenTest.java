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

package com.baidu.brpc.naming.consul;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import com.pszymczyk.consul.LogLevel;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
// @Ignore
public class ConsulNamingServiceTokenTest {

    private static ConsulNamingService consulNamingService;
    private static ConsulProcess consul;

    @BeforeClass
    public static void setUp() {
        String testToken = UUID.randomUUID().toString();

        final String customConfiguration = "{\n"
                + "\t\"acl\": {\n"
                + "\t\t\"enabled\": true,\n"
                + "\t\t\"default_policy\": \"deny\",\n"
                + "\t\t\"down_policy\": \"deny\","
                + "\t\t\"tokens\": {\n"
                + "\t\t\t\"agent\": \"" + testToken + "\",\n"
                + "\t\t\t\"master\": \"" + testToken + "\"\n"
                + "\t\t}\n"
                + "\t}\n"
                + "}";
        consul = ConsulStarterBuilder.consulStarter()
                .withLogLevel(LogLevel.DEBUG)
                .withCustomConfig(customConfiguration)
                .withConsulVersion("1.4.2")
                .withToken(testToken)
                .build()
                .start();
        BrpcURL namingUrl = new BrpcURL("consul://127.0.0.1:" + consul.getHttpPort() + "?token=" + testToken);
        consulNamingService = new ConsulNamingService(namingUrl);
    }

    @AfterClass
    public static void tearDown() {
        consulNamingService.destroy();
        consul.close();
        consulNamingService = null;
    }

    protected RegisterInfo createRegisterInfo(int port) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setHost("127.0.0.1");
        registerInfo.setPort(port);
        registerInfo.setInterfaceName(EchoService.class.getName());
        return registerInfo;
    }

    protected SubscribeInfo createSubscribeInfo(boolean ignoreFail) {
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setInterfaceName(EchoService.class.getName());
        subscribeInfo.setIgnoreFailOfNamingService(ignoreFail);
        return subscribeInfo;
    }

    @Test
    public void testLookup() throws InterruptedException {
        SubscribeInfo subscribeInfo = createSubscribeInfo(true);
        List<ServiceInstance> instances = consulNamingService.lookup(subscribeInfo);
        Assert.assertEquals(0, instances.size());

        RegisterInfo registerInfo = createRegisterInfo(8012);
        consulNamingService.register(registerInfo);
        Thread.sleep(3 * 1000);

        instances = consulNamingService.lookup(subscribeInfo);
        Assert.assertEquals(1, instances.size());
        Assert.assertEquals("127.0.0.1", instances.get(0).getIp());
        Assert.assertEquals(8012, instances.get(0).getPort());
        consulNamingService.unregister(registerInfo);
    }

    @Test(expected = RpcException.class)
    public void testWrongUrlWithoutToken() {
        BrpcURL wrongUrl = new BrpcURL("consul://127.0.0.1:" + consul.getHttpPort());
        ConsulNamingService wrongNamingService = new ConsulNamingService(wrongUrl);
        RegisterInfo registerInfo = createRegisterInfo(8012);
        wrongNamingService.register(registerInfo);
    }

    @Test
    public void testRegisterAndSubscribe() throws InterruptedException {
        RegisterInfo registerInfo = createRegisterInfo(8015);
        RegisterInfo anotherRegisterInfo = createRegisterInfo(8016);
        consulNamingService.register(registerInfo);
        consulNamingService.register(anotherRegisterInfo);
        Thread.sleep(3 * 1000);

        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        final List<ServiceInstance> adds = consulNamingService.lookup(subscribeInfo);
        Assert.assertEquals(2, adds.size());
        adds.clear();

        final List<ServiceInstance> deletes = new ArrayList<ServiceInstance>();
        consulNamingService.subscribe(subscribeInfo, new NotifyListener() {
            @Override
            public void notify(Collection<ServiceInstance> addList,
                               Collection<ServiceInstance> deleteList) {
                adds.addAll(addList);
                deletes.addAll(deleteList);
                log.info("notify: {}, {}", addList, deleteList);
            }
        });

        consulNamingService.unregister(registerInfo);
        Thread.sleep(3 * 1000);
        Assert.assertEquals(0, adds.size());
        Assert.assertEquals(1, deletes.size());
        Assert.assertEquals(deletes.get(0), new ServiceInstance("127.0.0.1", 8015));
        adds.clear();
        deletes.clear();

        consulNamingService.register(registerInfo);
        Thread.sleep(3 * 1000);
        Assert.assertEquals(1, adds.size());
        Assert.assertEquals(0, deletes.size());
        Assert.assertEquals(adds.get(0), new ServiceInstance("127.0.0.1", 8015));
    }

}
