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
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.heartbeat.HeartbeatService;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.transport.TransportFactory;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.core.filter.FilterChain;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.baidu.cloud.starlight.protocol.brpc.BrpcProtocol;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starlight Client associated with a single Server instance, facade of {@link ClientPeer} and {@link ClientInvoker} Can
 * generate Invoker, execute request asynchronous
 *
 * Created by liuruisen on 2019/12/6.
 */
public class SingleStarlightClient implements StarlightClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleStarlightClient.class);

    private final URI uri;

    // map of interfaceName and ClientInvoker
    private final Map<String, ClientInvoker> clientInvokers;

    private ClientPeer clientPeer;

    private AtomicBoolean isInitialed = new AtomicBoolean(false);

    private static volatile ThreadPoolFactory threadPoolOfAll;

    public SingleStarlightClient(String remoteIp, Integer remotePort, TransportConfig transportConfig) {
        this.clientInvokers = new ConcurrentHashMap<>();
        this.uri = assembleUri(remoteIp, remotePort, transportConfig);
        TransportFactory transportFactory =
            ExtensionLoader.getInstance(TransportFactory.class).getExtension(Constants.DEFAULT_TRANSPORT_FACTORY_NAME);
        this.clientPeer = transportFactory.client(uri);
    }

    @Override
    public URI remoteURI() {
        return this.uri;
    }

    @Override
    public void init() {
        if (threadPoolOfAll == null) {
            synchronized (SingleStarlightClient.class) {
                if (threadPoolOfAll == null) {
                    String bizThreadPoolName = uri.getParameter(Constants.BIZ_THREAD_POOL_NAME_KEY);
                    threadPoolOfAll =
                        ExtensionLoader.getInstance(ThreadPoolFactory.class).getExtension(bizThreadPoolName);
                    threadPoolOfAll.initDefaultThreadPool(uri, Constants.CLIENT_BIZ_THREAD_NAME_PREFIX);
                }
            }
        }
        if (isInitialed.compareAndSet(false, true)) {
            // Processor init
            Processor processor = new ClientProcessor(threadPoolOfAll);
            clientPeer.setProcessor(processor);
            clientPeer.init();
            try {
                long startTime = System.currentTimeMillis();
                clientPeer.connect();
                LOGGER.debug("Connect to remote cost: {}", System.currentTimeMillis() - startTime);
            } catch (Exception e) { // will reconnect in request stage
                LOGGER.warn("Connect to remote {} failed when init the single client, cause by {}", uri.getAddress(),
                    e.getMessage());
            }
            // refer heartbeat service
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setFilters(""); // zero filter
            refer(HeartbeatService.class, serviceConfig);
            updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));
        }
    }

    @Override
    public void refer(Class<?> serviceClass, ServiceConfig serviceConfig) {
        if (serviceClass.getInterfaces() != null && serviceClass.getInterfaces().length > 0) {
            // async future interface or callback interface
            if (serviceClass.getInterfaces().length > 1) {
                throw new StarlightRpcException(
                    "Starlight not support Multiple inheritance, " + "Only supports one layer of inheritance");
            }
            serviceClass = serviceClass.getInterfaces()[0];
        }

        String serviceName = serviceConfig.serviceName(serviceClass);
        if (clientInvokers.get(serviceName) != null) {
            return;
        }

        // get filters
        String filterNames = Constants.DEFAULT_CLIENT_FILTERS;
        if (serviceConfig != null) {
            filterNames = serviceConfig.getFilters() == null ? filterNames : serviceConfig.getFilters();
        }

        // Create Default ClientInvoker
        ClientInvoker clientInvoker =
            FilterChain.buildClientChainInvoker(new RpcClientInvoker(clientPeer, serviceConfig), filterNames);
        clientInvokers.put(serviceName, clientInvoker);
    }

    @Override
    public void request(Request request, RpcCallback callback) {

        request.setRemoteURI(uri);

        if (!isActive() && !HeartbeatService.HEART_BEAT_SERVICE_NAME.equals(request.getServiceName())) {
            LOGGER.warn("Receive request when Client is inactive, status {}, request {}, remoteAddress {}",
                clientPeer.status(), request, uri.getHost() + ":" + uri.getPort());
        }

        // set protocol and serviceConfig
        String protocolName = request.getProtocolName();
        if (StringUtils.isBlank(protocolName)) {
            ServiceConfig serviceConfig = request.getServiceConfig();
            protocolName = serviceConfig.getProtocol();
            request.setProtocolName(protocolName);
        }

        // check request
        try {
            checkRequest(request);
        } catch (StarlightRpcException e) {
            callback.onError(e);
            return;
        }

        try {
            String serviceName = request.getServiceName();
            ClientInvoker clientInvoker = clientInvokers.get(serviceName);
            clientInvoker.invoke(request, callback);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception was caught when request, request {}:{}", request.getServiceName(),
                request.getMethodName(), e);
            callback.onError(new StarlightRpcException(StarlightRpcException.UNKNOW,
                "Unhandled exception was caught when request, request " + request.getServiceName() + ":"
                    + request.getMethodName() + ", cause by " + e.getMessage()));
        }
    }

    private void checkRequest(Request request) throws StarlightRpcException {

        // check invoker
        String serviceName = request.getServiceName();
        ClientInvoker clientInvoker = clientInvokers.get(serviceName);
        if (clientInvoker == null) {
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "The request service has not been refer, please call refer() before request, " + "service {"
                    + serviceName + "}");
        }

        // check protocol
        String protocolName = request.getProtocolName();
        try {
            Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(protocolName);
            if (protocol == null) {
                throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                    "The protocol message in request is not support， protocolName {" + protocolName + "}");
            }
        } catch (Exception e) {
            if (e instanceof StarlightRpcException) {
                throw e;
            }
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "The protocol message in request is illegal, protocolName {" + protocolName + "}", e);
        }

        try {
            if (protocolName.equals(BrpcProtocol.PROTOCOL_NAME)) {
                BrpcProtocol.checkRequest(request);
            }
        } catch (StarlightRpcException e) {
            throw e;
        }
    }

    @Override
    public void destroy() {
        if (isInitialed.compareAndSet(true, false)) {
            StarlightStatsManager.removeStats(remoteURI());
            boolean gracefullyShutdown =
                uri.getParameter(Constants.GRACEFULLY_SHUTDOWN_ENABLE_KEY, Constants.GRACEFULLY_SHUTDOWN_ENABLE);
            if (clientPeer != null) {
                if (gracefullyShutdown) {
                    int quietPeriod = uri.getParameter(Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_KEY,
                        Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_VALUE);
                    int timeout = uri.getParameter(Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_KEY,
                        Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_VALUE);
                    clientPeer.gracefullyShutdown(quietPeriod, timeout);
                } else {
                    clientPeer.close();
                }
            }
            if (clientInvokers.size() > 0) {
                clientInvokers.clear();
            }
        }
    }

    /**
     * We use URI to store transport configuration
     * 
     * @param ip
     * @param port
     * @param config
     * @return
     */
    private URI assembleUri(String ip, Integer port, TransportConfig config) {
        URI.Builder uriBuilder = new URI.Builder(Constants.UNSPECIFIED_PROTOCOL, ip, port);
        uriBuilder.param(Constants.CONNECT_TIMEOUT_KEY, config.getConnectTimeoutMills() == null
            ? Constants.CONNECT_TIMEOUT_VALUE : config.getConnectTimeoutMills());
        uriBuilder.param(Constants.WRITE_TIMEOUT_KEY,
            config.getWriteTimeoutMills() == null ? Constants.WRITE_TIMEOUT_VALUE : config.getWriteTimeoutMills());
        uriBuilder.param(Constants.REQUEST_TIMEOUT_KEY, config.getRequestTimeoutMills() == null
            ? Constants.REQUEST_TIMEOUT_VALUE : config.getRequestTimeoutMills());
        uriBuilder.param(Constants.READ_IDLE_TIMEOUT_KEY,
            config.getReadIdleTimeout() == null ? Constants.READ_IDLE_TIMEOUT_VALUE : config.getReadIdleTimeout());
        uriBuilder.param(Constants.IO_THREADS_KEY,
            config.getIoThreadNum() == null ? Constants.DEFAULT_IO_THREADS_VALUE : config.getIoThreadNum());
        uriBuilder.param(Constants.MAX_HEARTBEAT_TIMES_KEY, config.getMaxHeartbeatTimes() == null
            ? Constants.MAX_HEARTBEAT_TIMES_VALUE : config.getMaxHeartbeatTimes());
        uriBuilder.param(Constants.RPC_CHANNEL_TYPE_KEY,
            config.getChannelType() == null ? Constants.DEFAULT_RPC_CHANNEL_TYPE_VALUE : config.getChannelType());
        uriBuilder.param(Constants.MAX_TOTAL_CONNECTIONS_KEY,
            config.getMaxConnections() == null ? Constants.MAX_TOTAL_CONNECTIONS : config.getMaxConnections());
        uriBuilder.param(Constants.MAX_IDLE_CONNECTIONS_KEY,
            config.getMaxIdleConnections() == null ? Constants.MAX_IDLE_CONNECTIONS : config.getMaxIdleConnections());
        uriBuilder.param(Constants.MIN_IDLE_CONNECTIONS_KEY,
            config.getMinIdleConnections() == null ? Constants.MIN_IDLE_CONNECTIONS : config.getMinIdleConnections());
        uriBuilder.param(Constants.GRACEFULLY_SHUTDOWN_ENABLE_KEY, config.getGracefullyShutdown() == null
            ? Constants.GRACEFULLY_SHUTDOWN_ENABLE : config.getGracefullyShutdown());
        uriBuilder.param(Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_KEY, config.getGracefullyQuietPeriod() == null
            ? Constants.GRACEFULLY_SHUTDOWN_QUIET_PERIOD_VALUE : config.getGracefullyQuietPeriod());
        uriBuilder.param(Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_KEY, config.getGracefullyTimeout() == null
            ? Constants.GRACEFULLY_SHUTDOWN_TIMEOUT_VALUE : config.getGracefullyTimeout());
        uriBuilder.param(Constants.CONNECT_KEEPALIVE_ENABLED_KEY, config.getConnectKeepAliveEnable() == null
            ? Constants.CONNECT_KEEPALIVE_ENABLED_VALUE : config.getConnectKeepAliveEnable());
        uriBuilder.param(Constants.MAX_BIZ_WORKER_NUM_KEY, config.getBizWorkThreadNum() == null
            ? Constants.DEFAULT_MAX_BIZ_THREAD_POOL_SIZE : config.getBizWorkThreadNum());
        uriBuilder.param(Constants.NETTY_IO_RATIO_KEY,
            config.getIoRatio() == null ? Constants.DEFAULT_NETTY_IO_RATIO : config.getIoRatio());
        uriBuilder.param(Constants.BIZ_THREAD_POOL_NAME_KEY, StringUtils.isEmpty(config.getBizThreadPoolName())
            ? Constants.DEFAULT_BIZ_THREAD_POOL_NAME : config.getBizThreadPoolName());

        if (config.getAdditional() != null) {
            uriBuilder.params(config.getAdditional());
        }

        return uriBuilder.build();
    }

    @Override
    public boolean isActive() {
        if (clientPeer == null) {
            return false;
        }

        if (clientPeer.status() == null) {
            return false;
        }

        return PeerStatus.Status.ACTIVE.equals(clientPeer.status().getStatus());
    }

    @Override
    public PeerStatus getStatus() {
        return this.clientPeer.status();
    }

    @Override
    public void updateStatus(PeerStatus newStatus) {
        this.clientPeer.updateStatus(newStatus);
    }
}
