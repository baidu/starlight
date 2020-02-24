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

package com.baidu.brpc.server;

import com.baidu.brpc.GovernanceSpiManager;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.protocol.*;
import com.baidu.brpc.utils.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wenweihu86 on 2017/4/24.
 */
@Getter
@Slf4j
public class RpcServer extends InterceptCommunicationServer {
    private NamingService namingService;
    private List<RegisterInfo> registerInfoList = new ArrayList<RegisterInfo>();
    private AtomicBoolean stop = new AtomicBoolean(false);

    public RpcServer(int port) {
        this(null, port, new RpcServerOptions(), null);
    }

    public RpcServer(String host, int port) {
        this(host, port, new RpcServerOptions(), null);
    }

    public RpcServer(int port, RpcServerOptions options) {
        this(null, port, options, null);
    }

    public RpcServer(String host, int port, RpcServerOptions options) {
        this(host, port, options, null);
    }

    public RpcServer(int port, RpcServerOptions options, List<Interceptor> interceptors) {
        this(null, port, options, interceptors);
    }

    public RpcServer(String host, int port,
                     final RpcServerOptions options,
                     List<Interceptor> interceptors) {
        super(host, port, options, interceptors);
        GovernanceSpiManager.getInstance().loadAllExtensions();
        if (StringUtils.isNotBlank(rpcServerOptions.getNamingServiceUrl())) {
            BrpcURL url = new BrpcURL(rpcServerOptions.getNamingServiceUrl());
            NamingServiceFactory namingServiceFactory = NamingServiceFactoryManager.getInstance()
                    .getNamingServiceFactory(url.getSchema());
            this.namingService = namingServiceFactory.createNamingService(url);
        }
    }

    /**
     * register service which can be accessed by client
     *
     * @param service       the service object which implement rpc interface.
     * @param namingOptions register center info
     * @param serverOptions service own custom RpcServerOptions
     *                      if not null, the service will not use the shared thread pool.
     */
    public void registerService(Object service, Class targetClass, NamingOptions namingOptions,
                                RpcServerOptions serverOptions) {
        super.registerService(service, targetClass, namingOptions, serverOptions);
        RegisterInfo registerInfo = null;
        if (namingOptions != null) {
            registerInfo = new RegisterInfo(namingOptions);
        } else {
            registerInfo = new RegisterInfo();
        }
        if (targetClass != null) {
            registerInfo.setInterfaceName(targetClass.getInterfaces()[0].getName());
        } else {
            registerInfo.setInterfaceName(service.getClass().getInterfaces()[0].getName());
        }
        registerInfo.setHost(NetUtils.getLocalAddress().getHostAddress());
        registerInfo.setPort(port);
        registerInfoList.add(registerInfo);
    }

    public void start() {
        super.start();
        if (namingService != null) {
            for (RegisterInfo registerInfo : registerInfoList) {
                registerInfo.setPort(port);
                namingService.register(registerInfo);
            }
            log.info("server register on success");
        }
    }

    public boolean shutdown() {
        if (super.shutdown()) {
            if (namingService != null) {
                for (RegisterInfo registerInfo : registerInfoList) {
                    namingService.unregister(registerInfo);
                }
                namingService.destroy();
            }
            return true;
        }
        return false;
    }

    public boolean isShutdown() {
        return stop.get();
    }

}
