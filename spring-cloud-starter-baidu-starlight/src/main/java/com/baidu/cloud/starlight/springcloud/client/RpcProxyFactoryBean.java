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

import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.core.rpc.proxy.JDKProxyFactory;
import com.baidu.cloud.starlight.api.rpc.proxy.ProxyFactory;
import com.baidu.cloud.starlight.springcloud.client.annotation.RpcProxy;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * RpcClientFactoryBean负责生成RpcProxy的代理Bean
 * <p>
 * Created by liuruisen on 2019-05-06.
 */
public class RpcProxyFactoryBean implements FactoryBean<Object>, InitializingBean {

    private Class<?> type;

    private Object proxy;

    private RpcProxy annotationInfos;

    private String clientName;

    private StarlightClient client;

    private StarlightClientProperties clientProperties;

    private ServiceConfig serviceConfig;

    @Override
    public Object getObject() throws Exception {
        if (proxy != null) {
            return proxy;
        }
        proxy = getProxy();
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // refer
        serviceConfig = new ServiceConfig();
        serviceConfig.setProtocol(getProtocol());
        serviceConfig.setCompressType(getCompressType());
        serviceConfig.setFilters(getFilters());
        serviceConfig.setServiceId(annotationInfos.serviceId());
        serviceConfig.setInvokeTimeoutMills(getRequestTimeout());
        serviceConfig.setSerializeMode(serializeMode());

        // get proxy
        proxy = getProxy();
    }

    private <T> T getProxy() {
        ProxyFactory proxyFactory = new JDKProxyFactory();
        return (T) proxyFactory.getProxy(type, serviceConfig, client);
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setAnnotationInfos(RpcProxy annotationInfos) {
        this.annotationInfos = annotationInfos;
    }

    public void setClient(StarlightClient client) {
        this.client = client;
    }

    public void setClientProperties(StarlightClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    private String getProtocol() {
        if (!StringUtils.isEmpty(annotationInfos.protocol())) { // Highest priority
            return annotationInfos.protocol();
        }

        return clientProperties.getProtocol(clientName);
    }

    private String getCompressType() {
        return clientProperties.getCompressType(clientName);
    }

    private String getFilters() {
        return clientProperties.getFilters(clientName);
    }

    private Integer getRequestTimeout() {
        return clientProperties.getRequestTimeoutMills(clientName, type.getName());
    }

    private String serializeMode() {
        return clientProperties.getSerializeMode(clientName);
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
