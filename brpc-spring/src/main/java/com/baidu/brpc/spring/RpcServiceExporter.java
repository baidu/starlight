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
package com.baidu.brpc.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * PBRPC exporter for standard PROTOBUF RPC implementation from jprotobuf-rpc-socket.
 *
 * @author xiemalin
 * @since 2.17
 */
@Setter
@Getter
@Slf4j
public class RpcServiceExporter extends RpcServerOptions implements InitializingBean, DisposableBean {
    /**
     * The pr rpc server.
     */
    private RpcServer prRpcServer;

    /**
     * The service port.
     */
    private int servicePort;

    /**
     * the register services which use default thread pool
     */
    private List<Object> registerServices = new ArrayList<Object>();

    /**
     * the register services which use individual thread pool
     */
    private Map<RpcServerOptions, Object> customOptionsServiceMap = new HashMap<RpcServerOptions, Object>();

    /**
     * {@link NamingOptions} for services
     */
    private Map<Object, NamingOptions> serviceNamingOptions = new HashMap<Object, NamingOptions>();

    /**
     * The interceptor.
     */
    private List<Interceptor> interceptors;

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    @Override
    public void destroy() throws Exception {
        if (prRpcServer != null) {
            prRpcServer.shutdown();
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.isTrue(servicePort > 0, "invalid service port: " + servicePort);
        if (registerServices.size() == 0 && customOptionsServiceMap.size() == 0) {
            throw new IllegalArgumentException("No register service specified.");
        }

        prRpcServer = new RpcServer(servicePort, this, interceptors);

        for (Object service : registerServices) {
            NamingOptions namingOptions = serviceNamingOptions.get(service);
            prRpcServer.registerService(service, AopUtils.getTargetClass(service), namingOptions, null);
        }

        for (Map.Entry<RpcServerOptions, Object> entry : customOptionsServiceMap.entrySet()) {
            NamingOptions namingOptions = serviceNamingOptions.get(entry.getValue());
            prRpcServer.registerService(entry.getValue(), AopUtils.getTargetClass(entry.getValue()), namingOptions,
                    entry.getKey());
        }

        prRpcServer.start();
    }

}
