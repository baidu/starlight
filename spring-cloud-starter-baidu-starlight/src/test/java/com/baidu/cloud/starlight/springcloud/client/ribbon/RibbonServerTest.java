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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import com.google.gson.Gson;
import org.junit.Test;
import org.springframework.cloud.consul.discovery.ConsulServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by liuruisen on 2021/5/18.
 */
public class RibbonServerTest {

    @Test
    public void starlightRibbonServerJson() {
        StarlightRibbonServer server = new StarlightRibbonServer("localhost", 8888);
        server.setMetadata(Collections.singletonMap("EM_PLATFORM", "test"));

        Gson gson = new Gson();
        String serverJson = gson.toJson(server);
        assertNotNull(serverJson);

        System.out.println(serverJson);

        StarlightRibbonServer serverResult = gson.fromJson(serverJson, StarlightRibbonServer.class);
        assertEquals(server.getMetadata(), serverResult.getMetadata());
        assertEquals(server.getHost(), serverResult.getHost());
        assertEquals(server.getPort(), serverResult.getPort());
        assertEquals(server.getId(), serverResult.getId());
        assertEquals(server.getScheme(), serverResult.getScheme());
    }

    @Test
    public void starlightRibbonServerJsonJackson() throws JsonProcessingException {
        List<StarlightRibbonServer> servers = ribbonServerList(5);

        ObjectMapper objectMapper = new ObjectMapper();
        String gravityServerJson = objectMapper.writeValueAsString(servers);
        assertNotNull(gravityServerJson);

        System.out.println(gravityServerJson);

        JavaType javaType =
            objectMapper.getTypeFactory().constructCollectionType(List.class, StarlightRibbonServer.class);
        try {
            List<StarlightRibbonServer> servers2 = objectMapper.readValue(gravityServerJson, javaType);
            assertEquals(5, servers2.size());
        } catch (IOException e) {
            // no default constructor
        }
    }

    private List<StarlightRibbonServer> ribbonServerList(int size) {

        List<StarlightRibbonServer> discoveryServers = new ArrayList<>();

        for (int i = 0; i < size; i++) {

            StarlightRibbonServer starlightRibbonServer = new StarlightRibbonServer("localhost", 8888 + i);
            starlightRibbonServer.setMetadata(Collections.singletonMap("EM_PLATFORM", "test"));

            discoveryServers.add(starlightRibbonServer);
        }

        return discoveryServers;
    }

    @Test
    public void consulServerJson() {
        HealthService healthService = new HealthService();
        HealthService.Service service = new HealthService.Service();
        service.setAddress("localhost");
        service.setPort(8888);
        service.setService("test-app");
        service.setCreateIndex(100L);
        service.setTags(Collections.singletonList("TEST=test"));
        healthService.setService(service);

        Check check = new Check();
        check.setCheckId("check-id");
        check.setStatus(Check.CheckStatus.PASSING);
        healthService.setChecks(Collections.singletonList(check));

        HealthService.Node node = new HealthService.Node();
        node.setId("397d2dd0-ece5-16c4-ced1-0a9084046bb6");
        node.setAddress("172.24.165.7");
        healthService.setNode(node);

        ConsulServer consulServer = new ConsulServer(healthService);
        consulServer.setId("172.24.165.7:8222");

        Gson gson = new Gson();
        String consulServerJson = gson.toJson(consulServer);
        assertNotNull(consulServerJson);

        ConsulServer consulServer2 = gson.fromJson(consulServerJson, ConsulServer.class);
        assertEquals(consulServer.getMetadata(), consulServer2.getMetadata());
        assertEquals(consulServer.getHost(), consulServer2.getHost());
        assertEquals(consulServer.getPort(), consulServer2.getPort());
        assertEquals(consulServer.getId(), consulServer2.getId());
        assertEquals(consulServer.getScheme(), consulServer2.getScheme());
    }
}
