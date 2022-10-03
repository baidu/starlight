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
 
package com.baidu.cloud.starlight.springcloud.client;

import com.baidu.cloud.starlight.springcloud.client.annotation.RpcProxy;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.server.service.TestService;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.NoOpPing;
import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListSubsetFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by liuruisen on 2020/3/25.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(value = {"spring.cloud.config.enabled=false", "spring.application.name=rpc-client",
    "spring.main.web-application-type=none", "starlight.client.config.default.cluster-model=failfast",
    "starlight.client.config.default.max-heartbeat-times=3", "starlight.client.config.default.warm-up-count=1",
    "starlight.client.config.test.warm-up-count=1", "starlight.client.config.test.compressType=none",
    "starlight.client.config.test.filters=test", "starlight.client.config.test.protocol=brpc",
    "spring.cloud.gravity.enable=false"}, classes = StarlightClientAutoConfigurationTest.StarlightClientApp.class)
public class StarlightClientAutoConfigurationTest {

    @Autowired
    private RpcProxyAnnotationBeanPostProcessor proxyAnnotationBeanPostProcessor;

    @Autowired
    private ApplicationContextUtils applicationContextUtils;

    @RpcProxy(remoteUrl = "brpc://localhost:8888")
    private TestService testService;

    @RpcProxy(name = "test", remoteUrl = "brpc://localhost:8888")
    private TestService testServiceSame;

    @Test
    public void autoConfiguration() {
        Assert.assertNotNull(proxyAnnotationBeanPostProcessor);
        Assert.assertNotNull(applicationContextUtils);
        Assert.assertNotNull(testService);
        Assert.assertNotNull(testServiceSame);
    }

    @SpringBootApplication
    @EnableDiscoveryClient(autoRegister = false)
    public static class StarlightClientApp {
        @Bean
        @ConditionalOnMissingBean
        public IClientConfig clientConfig() {
            return new DefaultClientConfigImpl();
        }

        @Bean
        @ConditionalOnMissingBean
        public ServerList serverList() {
            return new StaticServerList();
        }

        @Bean
        @ConditionalOnMissingBean
        public ServerListFilter serverListFilter() {
            return new ServerListSubsetFilter();
        }

        @Bean
        @ConditionalOnMissingBean
        public ServerListUpdater serverListUpdater() {
            return new PollingServerListUpdater();
        }

        @Bean
        @ConditionalOnMissingBean
        public IPing ping() {
            return new NoOpPing();
        }

        @Bean
        @ConditionalOnMissingBean
        public IRule rule() {
            return new RandomRule();
        }
    }

    {
        try {
            final ServerSocket serverSocket = new ServerSocket(8888);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    if (serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}