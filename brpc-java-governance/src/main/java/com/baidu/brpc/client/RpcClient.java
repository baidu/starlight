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

package com.baidu.brpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.baidu.brpc.CommunicationSpiManager;
import com.baidu.brpc.GovernanceSpiManager;
import com.baidu.brpc.client.channel.Endpoint;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.loadbalance.LoadBalanceManager;
import com.baidu.brpc.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.naming.NamingServiceProcessor;
import com.baidu.brpc.protocol.NamingOptions;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.thread.ShutDownManager;
import com.baidu.brpc.utils.CollectionUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by huwenwei on 2017/4/25.
 */
@SuppressWarnings("unchecked")
@Getter
@Slf4j
public class RpcClient {
    private RpcClientOptions rpcClientOptions = new RpcClientOptions();
    private CommunicationOptions communicationOptions;
    private LoadBalanceStrategy loadBalanceStrategy;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private NamingServiceProcessor namingServiceProcessor;
    private AtomicBoolean globalInit = new AtomicBoolean(false);
    private Class serviceInterface;
    private String namingServiceUrl;
    private List<Endpoint> endpoints;

    public RpcClient(String namingServiceUrl) {
        this(namingServiceUrl, new RpcClientOptions(), null);
    }

    public RpcClient(String namingServiceUrl, RpcClientOptions options) {
        this(namingServiceUrl, options, null);
    }

    /**
     * parse naming service url, connect to servers
     *
     * @param namingServiceUrl format like "list://127.0.0.1:8200"
     * @param options    rpc client options
     */
    public RpcClient(String namingServiceUrl,
                     final RpcClientOptions options,
                     List<Interceptor> interceptors) {
        Validate.notEmpty(namingServiceUrl);
        this.namingServiceUrl = namingServiceUrl;
        this.init(options, interceptors);
    }

    public RpcClient(Endpoint endPoint) {
        this(endPoint, null);
    }

    public RpcClient(Endpoint endPoint, RpcClientOptions options) {
        this(endPoint, options, null);
    }

    public RpcClient(Endpoint endPoint, RpcClientOptions options, List<Interceptor> interceptors) {
        this.endpoints = new ArrayList<Endpoint>(1);
        this.endpoints.add(endPoint);
        this.init(options, interceptors);
    }

    public RpcClient(List<Endpoint> endPoints) {
        this(endPoints, new RpcClientOptions(), null);
    }

    public RpcClient(List<Endpoint> endPoints, RpcClientOptions options) {
        this(endPoints, options, null);
    }

    public RpcClient(List<Endpoint> endPoints, RpcClientOptions options, List<Interceptor> interceptors) {
        Validate.notEmpty(endPoints);
        this.endpoints = endPoints;
        this.init(options, interceptors);
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz, NamingOptions namingOptions) {
        return BrpcProxy.getProxy(rpcClient, clazz, namingOptions);
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz) {
        return BrpcProxy.getProxy(rpcClient, clazz, null);
    }

    /**
     * @param service
     */
    public void registerPushService(Object service) {
        ServiceManager.getInstance().registerPushService(service);

        // 如果只注册了pushService，没有注册一个普通的服务的话， 报错
        if (namingServiceProcessor.getInstances().size() == 0) {
            log.error("there should be have normal services before register push service.");
            throw new RpcException("there should be have normal services before register push service");
        }
    }

    public <T> T getProxy(Class clazz, NamingOptions namingOptions) {
        return BrpcProxy.getProxy(this, clazz, namingOptions);
    }

    public <T> T getProxy(Class clazz) {
        return BrpcProxy.getProxy(this, clazz, null);
    }

    public void setServiceInterface(Class clazz) {
        setServiceInterface(clazz, null);
    }

    public void setServiceInterface(Class clazz, NamingOptions namingOptions) {
        if (this.serviceInterface != null) {
            throw new RpcException("serviceInterface must not be set repeatedly, please use another RpcClient");
        }
        if (clazz.getInterfaces().length == 0) {
            this.serviceInterface = clazz;
        } else {
            // if it is async interface, we should subscribe the sync interface
            this.serviceInterface = clazz.getInterfaces()[0];
        }
        if (StringUtils.isNoneBlank(namingServiceUrl)) {
            this.namingServiceProcessor = new NamingServiceProcessor(
                    namingServiceUrl, serviceInterface, namingOptions,
                    rpcClientOptions.getHealthyCheckIntervalMillis(), communicationOptions);
        } else {
            this.namingServiceProcessor = new NamingServiceProcessor(
                    endpoints, serviceInterface,
                    rpcClientOptions.getHealthyCheckIntervalMillis(), communicationOptions);
        }
    }

    public void shutdown() {
        stop();
    }

    public void stop() {
        // avoid stop multi times
        if (stop.compareAndSet(false, true)) {
            if (namingServiceProcessor != null) {
                namingServiceProcessor.stop();
            }
            if (loadBalanceStrategy != null) {
                loadBalanceStrategy.destroy();
            }
        }
    }

    public boolean isShutdown() {
        return stop.get();
    }

    public Response execute(Request request,
                            CommunicationOptions communicationOptions) throws RpcException {
        // load balance
        List<CommunicationClient> instances = namingServiceProcessor.getInstances();
        CommunicationClient client = loadBalanceStrategy.selectInstance(
                request, instances, request.getSelectedInstances());
        if (client == null) {
            log.warn("no available server instance");
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "no available server instance");
        }
        if (log.isDebugEnabled()) {
            log.debug("select instance {}", client.getServiceInstance());
        }

        Response response = communicationOptions.getProtocol().createResponse();
        client.executeChain(request, response);
        return response;
    }

    private void init(final RpcClientOptions options, List<Interceptor> interceptors) {

        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<Interceptor>();
        }

        if (null == options) {
            rpcClientOptions = new RpcClientOptions();
        } else {
            try {
                rpcClientOptions.copyFrom(options);
            } catch (Exception ex) {
                log.warn("init rpc options failed, so use default");
                rpcClientOptions = new RpcClientOptions();
            }
        }

        initGlobal(rpcClientOptions);
        communicationOptions = rpcClientOptions.buildCommunicationOptions(interceptors);

        // 负载均衡算法
        loadBalanceStrategy = LoadBalanceManager.getInstance().createLoadBalance(
                rpcClientOptions.getLoadBalanceType());
        loadBalanceStrategy.init(this);
    }

    public void initGlobal(RpcClientOptions options) {
        if (globalInit.compareAndSet(false, true)) {
            CommunicationSpiManager.getInstance().loadAllExtensions(options.getEncoding());
            GovernanceSpiManager.getInstance().loadAllExtensions();
            FastFutureStore.getInstance(options.getFutureBufferSize());
            ShutDownManager.getInstance();
        }
    }

}
