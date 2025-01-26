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
 
package com.baidu.cloud.starlight.springcloud.server.register.consul;

import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterListener;
import com.ecwid.consul.v1.agent.model.NewService;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Register Starlight Service to Consul Created by liuruisen on 2020/3/2.
 */
public class StarlightConsulRegisterListener extends StarlightRegisterListener {

    @Override
    protected Registration createStarlightRegistration() {
        ConsulDiscoveryProperties discoveryProperties = applicationContext.getBean(ConsulDiscoveryProperties.class);
        HeartbeatProperties heartbeatProperties = applicationContext.getBean(HeartbeatProperties.class);
        // Create NewService
        String appName = getAppName(applicationContext.getEnvironment()); // appName
        NewService service = new NewService();
        service.setName(appName); // name

        service.setId(getInstanceId(applicationContext.getEnvironment())); // instanceId
        if (discoveryProperties.getInstanceId() != null) {
            service.setId(discoveryProperties.getInstanceId());
        }

        if (!discoveryProperties.isPreferAgentAddress()) {
            service.setAddress(discoveryProperties.getHostname()); // address
        }

        service.setPort(getPort(applicationContext.getEnvironment())); // port

        service.setMeta(getMetadata(discoveryProperties));
        service.setCheck(createCheck(service.getPort(), heartbeatProperties, discoveryProperties)); // rpc check

        return new ConsulRegistration(service, discoveryProperties);
    }

    /***
     * Rpc service Check
     * 
     * @return
     */
    public static NewService.Check createCheck(Integer port, HeartbeatProperties ttlConfig,
        ConsulDiscoveryProperties properties) {
        NewService.Check check = new NewService.Check();
        if (ttlConfig.isEnabled()) {
            check.setTtl(ttlConfig.getTtl().getSeconds() + "s");
            // Note. Default timeout to deregister services critical for longer than timeout (e.g. 3m).
            check.setDeregisterCriticalServiceAfter("3m");
            if (StringUtils.hasText(properties.getHealthCheckCriticalTimeout())) {
                check.setDeregisterCriticalServiceAfter(properties.getHealthCheckCriticalTimeout());
            }
            return check;
        }

        Assert.notNull(port, "createCheck port must not be null");
        Assert.isTrue(port > 0, "createCheck port must be greater than 0");

        // TCP Check
        check.setTcp(String.format("%s:%s", properties.getHostname(), port));

        check.setInterval(properties.getHealthCheckInterval());
        check.setTimeout(properties.getHealthCheckTimeout());
        if (StringUtils.hasText(properties.getHealthCheckCriticalTimeout())) {
            check.setDeregisterCriticalServiceAfter(properties.getHealthCheckCriticalTimeout());
        }
        check.setTlsSkipVerify(properties.getHealthCheckTlsSkipVerify());
        return check;
    }

    private Map<String, String> getMetadata(ConsulDiscoveryProperties properties) {
        Map<String, String> metadata = new HashMap<>();
        if (!CollectionUtils.isEmpty(properties.getMetadata())) {
            metadata.putAll(properties.getMetadata());
        }

        // add metadata from other properties. See createTags above.
        if (!StringUtils.hasLength(properties.getInstanceZone())) {
            metadata.put(properties.getDefaultZoneMetadataName(), properties.getInstanceZone());
        }
        if (!StringUtils.hasLength(properties.getInstanceGroup())) {
            metadata.put("group", properties.getInstanceGroup());
        }

        // store the secure flag in the tags so that clients will be able to figure
        // out whether to use http or https automatically
        metadata.put("secure", Boolean.toString(properties.getScheme().equalsIgnoreCase("https")));

        // add starlight metadata
        metadata.putAll(starlightMetas());

        return metadata;
    }

}