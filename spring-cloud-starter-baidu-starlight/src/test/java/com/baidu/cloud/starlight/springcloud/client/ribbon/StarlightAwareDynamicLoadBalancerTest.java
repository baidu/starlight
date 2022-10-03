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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.netflix.loadbalancer.Server;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Date 2022/9/27 15:57
 * @Created by liuruisen
 */
public class StarlightAwareDynamicLoadBalancerTest {

    private Map<Class, Method> metadateMethods = new HashMap<>();

    @Test
    public void testSetServersList() {
        // 500
        List<Server> servers = ribbonServers(500);

        long startTime = System.currentTimeMillis();
        List<StarlightRibbonServer> starlightRibbonServers = covert(servers);
        System.out.println("500 cost : " + (System.currentTimeMillis() - startTime));

        // 1000
        List<Server> servers_1000 = ribbonServers(1000);

        long startTime_1000 = System.currentTimeMillis();
        covert(servers_1000);
        System.out.println("1000 cost : " + (System.currentTimeMillis() - startTime_1000));

        // 2000
        List<Server> servers_2000 = ribbonServers(2000);

        long startTime_2000 = System.currentTimeMillis();
        covert(servers_2000);
        System.out.println("2000 cost : " + (System.currentTimeMillis() - startTime_2000));

        // 5000
        List<Server> servers_5000 = ribbonServers(5000);

        long startTime_5000 = System.currentTimeMillis();
        covert(servers_5000);
        System.out.println("5000 cost : " + (System.currentTimeMillis() - startTime_5000));

        // 10000
        List<Server> servers_10000 = ribbonServers(10000);

        long startTime_10000 = System.currentTimeMillis();
        covert(servers_10000);
        System.out.println("10000 cost : " + (System.currentTimeMillis() - startTime_10000));
    }

    // consul nacos gravity 不支持eruaka zookeeper
    private List<Server> ribbonServers(int num) {
        List<Server> servers = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            CustomServer server = new CustomServer("127.0.0.1", i);
            servers.add(server);
        }

        return servers;
    }

    private List<StarlightRibbonServer> covert(List<Server> servers) {
        return servers.stream().map(server -> {
            StarlightRibbonServer starlightRibbonServer = new StarlightRibbonServer(server.getHost(), server.getPort());
            Class clazz = server.getClass();
            Method metatdataMethod = metadateMethods.computeIfAbsent(clazz, aClass -> {
                Method method = null;
                try {
                    method = clazz.getDeclaredMethod("getMetadata");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                return method;
            });
            if (metatdataMethod != null) {
                Map<String, String> metadata = null;
                try {
                    metadata = (Map<String, String>) metatdataMethod.invoke(server);
                    starlightRibbonServer.setMetadata(metadata);
                } catch (Exception e) {
                    starlightRibbonServer.setMetadata(new HashMap<>());
                }
            }
            return starlightRibbonServer;
        }).collect(Collectors.toList());
    }

    private static class CustomServer extends Server {

        private static final String value = "fguoasd;iqowoepqwepgqweuqwgphqwueqwuxroqinywr2wynerxoqyer0oqpooerumqcwry"
            + "ashldhasfldshflkasyh9ewpaklhfasyfsakljfiausdfuj'as;jfwqmurpqmwefpqwprpjqwiopfwqe"
            + "ahfoipfopadfprqu9wurqpoewjiodcyuqewuru[ewoqjpoqweu9pwermp908c908qwm09rm9i y849m5cup9mcrwy8qm2";

        private Map<String, String> metadata;

        public CustomServer(String host, int port) {
            super(host, port);
            this.metadata = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                metadata.put("Key" + i, value);
            }
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }

    private static class StarlightRibbonServer extends Server {

        private Map<String, String> metadata;

        public StarlightRibbonServer(String host, int port) {
            super(host, port);
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }
}