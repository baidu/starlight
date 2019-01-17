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

import java.util.List;

import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * PBRPC exporter for standard PROTOBUF RPC implementation from jprotobuf-rpc-socket.
 * 
 * @author xiemalin
 * @since 2.17
 */
@Setter
@Getter
public class RpcServiceExporter extends RpcServerOptions implements InitializingBean, DisposableBean {

    /** The pr rpc server. */
    private RpcServer prRpcServer;
    
    /** The service port. */
    private int servicePort;

    /** The registry center service. */
    private NamingServiceFactory namingServiceFactory;

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
    
    /** The register services. */
    private List<Object> registerServices;
    
	/** The interceptor. */
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
        Assert.isTrue(!CollectionUtils.isEmpty(registerServices), "No register service specified.");
        
        prRpcServer = new RpcServer(servicePort, this, interceptors, namingServiceFactory);
        NamingOptions namingOptions = new NamingOptions();
        namingOptions.setGroup(group);
        namingOptions.setVersion(version);
        namingOptions.setIgnoreFailOfNamingService(ignoreFailOfNamingService);
        for (Object service : registerServices) {
            prRpcServer.registerService(service, namingOptions);
        }
        prRpcServer.start();
    }

}
