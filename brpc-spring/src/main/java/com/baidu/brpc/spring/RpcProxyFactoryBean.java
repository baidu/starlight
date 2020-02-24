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
package com.baidu.brpc.spring;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.NamingOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * {@link FactoryBean} for PbRpc proxies.
 *
 * @author xiemalin
 * @since 2.0.2
 */
@Setter
@Getter
public class RpcProxyFactoryBean extends RpcClientOptions
        implements FactoryBean, InitializingBean, DisposableBean {
    
    /** The service interface. */
    private Class serviceInterface;

    /** naming service url */
    private String namingServiceUrl;

    /**
     * identify different service implementation for the same interface.
     */
    private String group = "normal";

    /**
     * identify service version.
     */
    private String version = "1.0.0";

    /**
     * if true, naming service will throw exception when register/subscribe exceptions.
     */
    private boolean ignoreFailOfNamingService = false;

    /**
     * use serviceId to identify all instances for this service.
     */
    private String serviceId;
    
	/** The interceptors. */
	private List<Interceptor> interceptors;

    /** The service proxy. */
    private Object serviceProxy;

    /** The rpc client. */
    private RpcClient rpcClient;

    /**
     * Sets the service interface.
     *
     * @param serviceInterface the new service interface
     */
    public void setServiceInterface(Class serviceInterface) {
        if (serviceInterface != null && !serviceInterface.isInterface()) {
            throw new IllegalArgumentException("'serviceInterface' must be an interface");
        }
        this.serviceInterface = serviceInterface;
    }

    /**
     * Gets the service interface.
     *
     * @return the service interface
     */
    public Class getServiceInterface() {
        return this.serviceInterface;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    @Override
    public Object getObject() throws Exception {
        return serviceProxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    @Override
    public Class getObjectType() {
        return serviceInterface;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (rpcClient == null) {
            rpcClient = new RpcClient(namingServiceUrl, this, interceptors);
        }
        NamingOptions namingOptions = new NamingOptions();
        namingOptions.setGroup(group);
        namingOptions.setVersion(version);
        namingOptions.setIgnoreFailOfNamingService(ignoreFailOfNamingService);
        namingOptions.setServiceId(serviceId);
        this.serviceProxy = BrpcProxy.getProxy(rpcClient, serviceInterface, namingOptions);
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    @Override
    public void destroy() throws Exception {
        if (rpcClient != null) {
            rpcClient.stop();
        }
    }

}
