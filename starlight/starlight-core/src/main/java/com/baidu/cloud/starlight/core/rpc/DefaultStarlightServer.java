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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.heartbeat.HeartbeatService;
import com.baidu.cloud.starlight.api.heartbeat.HeartbeatServiceImpl;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.api.utils.EnvUtils;
import com.baidu.cloud.starlight.core.filter.FilterChain;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.threadpool.RpcThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.ServerPeer;
import com.baidu.cloud.starlight.transport.StarlightTransportFactory;
import com.baidu.cloud.starlight.api.transport.TransportFactory;
import com.baidu.cloud.starlight.protocol.http.springrest.SpringRestHandlerMapping;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by liuruisen on 2020/1/13.
 */
public class DefaultStarlightServer implements StarlightServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStarlightServer.class);

    private ServerPeer serverPeer;

    private URI uri;

    public DefaultStarlightServer(String protocol, String host, Integer port, TransportConfig transportConfig) {
        if (StringUtils.isBlank(protocol)) {
            this.uri = assembleUri(null, host, port, transportConfig);
        } else {
            this.uri = assembleUri(protocol, host, port, transportConfig);
        }
        TransportFactory transportFactory = new StarlightTransportFactory();
        this.serverPeer = transportFactory.server(uri);
    }

    public DefaultStarlightServer(String host, Integer port, TransportConfig transportConfig) {
        this.uri = assembleUri(null, host, port, transportConfig);
        TransportFactory transportFactory = new StarlightTransportFactory();
        this.serverPeer = transportFactory.server(uri);
    }

    @Override
    public void init() {
        // <1> Processor
        int maxBizWorkerNum =
            uri.getParameter(Constants.MAX_BIZ_WORKER_NUM_KEY, Constants.DEFAULT_MAX_BIZ_THREAD_POOL_SIZE);
        Processor processor = new ServerProcessor(RpcServiceRegistry.getInstance(),
            new RpcThreadPoolFactory(Constants.DEFAULT_BIZ_THREAD_POOL_SIZE, maxBizWorkerNum, "s")); // s:server
        // <3> init ServerPeer
        serverPeer.init();
        serverPeer.setProcessor(processor);
        // export heartbeat service
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setFilters(""); // no filter
        export(HeartbeatService.class, new HeartbeatServiceImpl(), serviceConfig);
    }

    @Override
    public void destroy() {
        boolean gracefullyShutdown =
            uri.getParameter(Constants.GRACEFULLY_SHUTDOWN_ENABLE_KEY, Constants.GRACEFULLY_SHUTDOWN_ENABLE);

        // close serverPeer
        if (serverPeer != null) {
            if (gracefullyShutdown) {
                int quietPeriod = uri.getParameter(Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_KEY,
                    Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_VALUE);
                int timeout = uri.getParameter(Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_KEY,
                    Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_VALUE);
                serverPeer.gracefullyShutdown(quietPeriod, timeout);
            } else {
                serverPeer.close();
            }
        }
        ServiceRegistry serviceRegistry = RpcServiceRegistry.getInstance();
        serviceRegistry.destroy();
    }

    @Override
    public void serve() {
        serverPeer.bind();
    }

    @Override
    public void export(Class<?> rpcInterface, Object rpcObject) {
        // Init RpcService by rpcInterface and rpcObject
        RpcService rpcService = new RpcService(rpcInterface, rpcObject);
        // export rpcService
        export(rpcService);
    }

    @Override
    public void export(Class<?> rpcInterface, Object rpcObject, ServiceConfig serviceConfig) {
        // Init RpcService by rpcInterface and rpcObject
        RpcService rpcService = new RpcService(rpcInterface, rpcObject, serviceConfig);
        // export rpcService
        export(rpcService);
    }

    @Override
    public void export(RpcService rpcService) {
        // Register {@link RpcService} to {@link ServiceRegistry}
        ServiceRegistry serviceRegistry = RpcServiceRegistry.getInstance();
        // <2> init ServerInvoker
        String filterNames = Constants.DEFAULT_SERVER_FILTERS;
        ServiceConfig serviceConfig = rpcService.getServiceConfig();
        if (serviceConfig != null) {
            filterNames = serviceConfig.getFilters() == null ? filterNames : serviceConfig.getFilters();
        }
        ServiceInvoker serviceInvoker =
            FilterChain.buildServerChainInvoker(new RpcServiceInvoker(rpcService), filterNames);
        serviceRegistry.register(serviceInvoker);

        // create request mapping info, when service is spring rest type
        SpringRestHandlerMapping handlerMapping = SpringRestHandlerMapping.getInstance();
        handlerMapping.createMapping(rpcService.getServiceClass(), rpcService.getServiceObject());

        LOGGER.info("Export RpcService success, RpcService: {}", rpcService);
    }

    @Override
    public void unexport(Class<?> rpcInterface) {
        ServiceRegistry serviceRegistry = RpcServiceRegistry.getInstance();
        ServiceInvoker serviceInvoker = serviceRegistry.discover(rpcInterface.getName());
        if (serviceInvoker == null) {
            return;
        }
        unexport(serviceInvoker.getRpcService());
    }

    @Override
    public void unexport(RpcService rpcService) {
        ServiceRegistry serviceRegistry = RpcServiceRegistry.getInstance();
        serviceRegistry.unRegister(serviceRegistry.discover(rpcService.getServiceName()));
    }

    private URI assembleUri(String protocol, String host, Integer port, TransportConfig config) {
        if (StringUtils.isBlank(host)) {
            host = Constants.ANYHOST_VALUE;
        }
        URI.Builder uriBuilder = new URI.Builder(Constants.UNSPECIFIED_PROTOCOL, host, port);
        if (!StringUtils.isBlank(protocol)) {
            uriBuilder = new URI.Builder(protocol, host, port);
        }
        uriBuilder.param(Constants.CONNECT_TIMEOUT_KEY, config.getConnectTimeoutMills() == null
            ? Constants.CONNECT_TIMEOUT_VALUE : config.getConnectTimeoutMills());
        uriBuilder.param(Constants.WRITE_TIMEOUT_KEY,
            config.getWriteTimeoutMills() == null ? Constants.WRITE_TIMEOUT_VALUE : config.getWriteTimeoutMills());
        uriBuilder.param(Constants.ALL_IDLE_TIMEOUT_KEY,
            config.getAllIdleTimeout() == null ? Constants.ALL_IDLE_TIMEOUT_VALUE : config.getAllIdleTimeout());
        uriBuilder.param(Constants.IO_THREADS_KEY,
            config.getIoThreadNum() == null ? Constants.DEFAULT_IO_THREADS_VALUE : config.getIoThreadNum());
        uriBuilder.param(Constants.ACCEPT_THREADS_KEY, config.getAcceptThreadNum() == null
            ? Constants.DEFAULT_ACCEPTOR_THREAD_VALUE : config.getAcceptThreadNum());
        uriBuilder.param(Constants.GRACEFULLY_SHUTDOWN_ENABLE_KEY, config.getGracefullyShutdown() == null
            ? Constants.GRACEFULLY_SHUTDOWN_ENABLE : config.getGracefullyShutdown());
        uriBuilder.param(Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_KEY, config.getGracefullyQuietPeriod() == null
            ? Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_VALUE : config.getGracefullyQuietPeriod());
        uriBuilder.param(Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_KEY, config.getGracefullyTimeout() == null
            ? Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_VALUE : config.getGracefullyTimeout());
        uriBuilder.param(Constants.CONNECT_KEEPALIVE_ENABLED_KEY, config.getConnectKeepAliveEnable() == null
            ? Constants.CONNECT_KEEPALIVE_ENABLED_VALUE : config.getConnectKeepAliveEnable());
        uriBuilder.param(Constants.MAX_BIZ_WORKER_NUM_KEY,
            config.getBizWorkThreadNum() == null ? maxBizThreadNum() : config.getBizWorkThreadNum());
        uriBuilder.param(Constants.NETTY_IO_RATIO_KEY,
            config.getIoRatio() == null ? Constants.DEFAULT_NETTY_IO_RATIO : config.getIoRatio());
        return uriBuilder.build();
    }

    private int maxBizThreadNum() {
        int maxBizWorkerNum = EnvUtils.getContainerThreadNum(Constants.DEFAULT_MAX_BIZ_THREAD_POOL_SIZE);
        return maxBizWorkerNum;
    }
}
