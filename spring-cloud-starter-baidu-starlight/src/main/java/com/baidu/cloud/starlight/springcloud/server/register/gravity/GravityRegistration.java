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
 
package com.baidu.cloud.starlight.springcloud.server.register.gravity;

import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.Map;

/**
 * gravity-spring-cloud-starter support convert Registration to Gravity Endpoint
 * so we implements registration as GravityRegistration
 * Created by liuruisen on 2020/8/13.
 */
public class GravityRegistration implements Registration {

    private String serviceId;

    private String host;

    private Integer port;

    private String schema;

    private Map<String, String> metadata;

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public URI getUri() {
        String uri = String.format("%s://%s:%s", schema, host, port);
        return URI.create(uri);
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String getScheme() {
        return schema;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
