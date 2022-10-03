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
 
package com.baidu.cloud.starlight.springcloud.client.cluster;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.LabelClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.LabelSelectorRouter;
import com.baidu.cloud.starlight.springcloud.client.properties.OutlierConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.configuration.Configuration;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by liuruisen on 2020/1/4.
 */
public abstract class AbstractClusterClient implements StarlightClient {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final String name; // spring cloud application name: {spring.application.name}

    private final LoadBalancer loadBalancer; // Choose ServiceInstance

    private final DiscoveryClient discoveryClient; // get instance from Registry Center

    protected final StarlightClientProperties properties;

    private final SingleStarlightClientManager clientManager;

    private final Map<Class<?>, ServiceConfig> serviceConfigs; // concurrentHashMap

    private final TransportConfig transportConfig;

    private volatile PeerStatus peerStatus;

    private final Map<Request, AtomicInteger> netErrorRetryTimes;

    private RouterChain routerChain;

    private Configuration configuration;

    private StarlightRouteProperties routeProperties;

    private Set<ServiceInstance> initedInstances = new HashSet<>();

    /**
     * 构造注入，1是符合spring 4.x之后的推荐，2是给海若构造使用
     * 
     * @param name
     * @param properties
     * @param loadBalancer
     * @param discoveryClient
     * @param clientManager
     */
    public AbstractClusterClient(String name, StarlightClientProperties properties, LoadBalancer loadBalancer,
        DiscoveryClient discoveryClient, SingleStarlightClientManager clientManager, Configuration configuration,
        StarlightRouteProperties routeProperties) {
        this.name = name;
        this.properties = properties;
        this.loadBalancer = loadBalancer;
        this.discoveryClient = discoveryClient;
        this.serviceConfigs = new ConcurrentHashMap<>();
        this.transportConfig = properties.transportConfig(name);
        this.clientManager = clientManager;
        this.configuration = configuration;
        this.routeProperties = routeProperties;
        this.netErrorRetryTimes = new ConcurrentHashMap<>();
    }

    public String getName() {
        return name;
    }

    @Override
    public void init() {
        LOGGER.info("Init starlight client {}", getName());
        if (properties.getWarmUpEnabled(getName())) {
            LOGGER.info("Warm up starlight client {}", getName());
            List<ServiceInstance> instances = discoveryClient.getInstances(name);
            if (instances != null && instances.size() > 0) { // preloading
                int warmUpCount = warmUpSize(instances.size());
                for (int i = 0; i < warmUpCount; i++) {
                    try {
                        ServiceInstance serviceInstance = instances.get(i);
                        initSingleClient(serviceInstance.getHost(), serviceInstance.getPort());
                        initedInstances.add(serviceInstance);
                    } catch (Exception e) {
                        LOGGER.error("Failed to init SingleClient in ClusterClient#init() method, "
                            + "will retry to init in ClusterClient#request() method", e);
                    }
                }
            }
        }

        LabelSelectorRouter selectorRouter =
            new LabelSelectorRouter(getName(), routeProperties, properties, loadBalancer);
        List<Router> routes = new ArrayList<>();
        if (routeProperties != null && routeProperties.getEnabled()) {
            routes.add(selectorRouter);
        } else {
            LOGGER.info("Will not execute xds route because route.enabled is false");
        }

        routerChain = new RouterChain(routes, selectorRouter);

        this.peerStatus = new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis());
    }

    protected SingleStarlightClient initSingleClient(String host, Integer port) {
        OutlierConfig outlierConfig = properties.getOutlierConfig(getName());
        if (outlierConfig != null) {
            Map<String, String> transConfigAdd = new HashMap<>();
            transConfigAdd.put(SpringCloudConstants.OUTLIER_DETECT_ENABLED_KEY,
                String.valueOf(outlierConfig.getEnabled()));
            transConfigAdd.put(SpringCloudConstants.OUTLIER_DETECT_INTERVAL_KEY,
                String.valueOf(outlierConfig.getDetectInterval()));
            transConfigAdd.put(SpringCloudConstants.OUTLIER_DETECT_MINI_REQUEST_NUM_KEY,
                String.valueOf(outlierConfig.getFailurePercentMinRequest()));
            transConfigAdd.put(SpringCloudConstants.OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY,
                String.valueOf(outlierConfig.getFailurePercentThreshold()));
            if (outlierConfig.getFailureCountThreshold() != null) {
                transConfigAdd.put(SpringCloudConstants.OUTLIER_DETECT_FAIL_COUNT_THRESHOLD_KEY,
                    String.valueOf(outlierConfig.getFailureCountThreshold()));
            }
            transportConfig.setAdditional(transConfigAdd);
        }

        return clientManager.getOrCreateSingleClient(host, port, transportConfig);
    }

    @Override
    public void request(Request request, RpcCallback callback) {
        callback = new NetworkErrorRetryCallback(callback);
        addNetErrorRetryTimes(request); // retry when network error
        addProviderAppName(request);
        // sub cluster execute
        boolean labelRouter = false;
        RequestContext requestContext = new RequestContext(request, RpcContext.getContext());
        try {
            // route
            Cluster cluster = routerChain.route(requestContext);
            if (cluster.getClusterSelector() instanceof LabelClusterSelector) {
                labelRouter = true;
            }
            // 此处set由volatile保证可见性，应不涉及线程并发问题
            cluster.setServiceRefers(serviceConfigs);
            cluster.execute(request, callback);
        } catch (Throwable e) {
            // not enable fallback, throw
            if (routeProperties.getNoInstanceFallBack() == null || !routeProperties.getNoInstanceFallBack()) {
                LOGGER.error("Request failed and cannot fallback, req:{}#{}, caused by", request.getServiceName(),
                    request.getMethodName(), e);
                throw e;
            }
            // fallback
            if (e instanceof StarlightRpcException
                && SpringCloudConstants.NO_INSTANCE_ERROR_CODE.equals(((StarlightRpcException) e).getCode())
                && !labelRouter) {
                LOGGER.info("No instance found from the routed cluster, fallback to the label selector route");
                Cluster cluster = routerChain.noneRoute(requestContext);
                // 此处set由volatile保证可见性，应不涉及线程并发问题
                cluster.setServiceRefers(serviceConfigs);
                cluster.execute(request, callback);
            } else {
                LOGGER.error("Request failed, req:{}#{}, caused by", request.getServiceName(), request.getMethodName(),
                    e);
                throw e;
            }
        }
    }

    // generate ClientInvoker for all Instances(Ip + port)
    @Override
    public void refer(Class<?> serviceClass, ServiceConfig serviceConfig) {
        if (serviceConfigs.get(serviceClass) == null) {
            LOGGER.info("Refer service class {}", serviceClass.getName());
            serviceConfigs.put(serviceClass, serviceConfig);
            for (ServiceInstance instance : initedInstances) {
                SingleStarlightClient client = clientManager.getSingleClient(instance.getHost(), instance.getPort());
                if (client != null) {
                    client.refer(serviceClass, serviceConfig);
                }
            }
        }
    }

    @Override
    public void destroy() {
        if (serviceConfigs != null && serviceConfigs.size() > 0) {
            serviceConfigs.clear();
        }
    }

    private int warmUpSize(Integer instanceSize) {
        Integer warmUpRatio = properties.getWarmUpRatio(getName());
        Integer warmUpCount = properties.getWarmUpCount(getName());

        if (warmUpCount != null) {
            return warmUpCount;
        }

        if (warmUpRatio != null) {
            return instanceSize * warmUpRatio / 100;
        }

        return instanceSize;
    }

    private void addNetErrorRetryTimes(Request request) {
        netErrorRetryTimes.putIfAbsent(request, new AtomicInteger(properties.getNetworkErrorRetryTimes(getName())));
    }

    private void removeNetErrorRetryTimes(Request request) {
        netErrorRetryTimes.remove(request);
    }

    private void addProviderAppName(Request request) {
        if (request.getAttachmentKv() == null) {
            request.setAttachmentKv(new HashMap<>());
        }
        // used in rate limiter filter
        request.getAttachmentKv().put(Constants.PROVIDER_APP_NAME_KEY, getName());
        try {
            // used for logging
            request.getAttachmentKv().put(Constants.CONSUMER_APP_NAME_KEY,
                ApplicationContextUtils.getApplicationName());
        } catch (Exception e) {
            LOGGER.warn("Get appName failed, do not need to pay attention, appName will be used for logging. msg {}",
                e.getMessage());
        }
    }

    @Override
    public boolean isActive() {
        for (Map.Entry<String, SingleStarlightClient> entry : clientManager.allSingleClients().entrySet()) {
            if (entry.getValue().isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PeerStatus getStatus() {
        return this.peerStatus;
    }

    @Override
    public void updateStatus(PeerStatus newStatus) {
        throw new RuntimeException("ClusterStarlightClient not support update status");
    }

    @Override
    public URI remoteURI() {
        throw new UnsupportedOperationException("Get remoteURI is not support in AbstractClusterClient");
    }

    private class NetworkErrorRetryCallback implements RpcCallback {

        private final RpcCallback chainedCallback;

        public NetworkErrorRetryCallback(RpcCallback callback) {
            chainedCallback = callback;
        }

        @Override
        public void addTimeout(Timeout timeout) {
            chainedCallback.addTimeout(timeout);
        }

        @Override
        public Request getRequest() {
            return chainedCallback.getRequest();
        }

        @Override
        public void onResponse(Response response) {
            removeNetErrorRetryTimes(getRequest());
            chainedCallback.onResponse(response);
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof TransportException && netErrorRetryTimes.get(getRequest()) == null) {
                LOGGER.warn(
                    "Request to {} failed caused by network error {}, configured retryTimes {}, "
                        + "reqId {}, mapSize {}",
                    getRequest().getRemoteURI().getAddress(), ((TransportException) e).getCode(),
                    netErrorRetryTimes.get(getRequest()), getRequest().getId(), netErrorRetryTimes.size());
            }
            if (e instanceof TransportException && netErrorRetryTimes.get(getRequest()) != null) {
                int retryTimes = netErrorRetryTimes.get(getRequest()).getAndDecrement();
                if (retryTimes > 0) {
                    LOGGER.info("Request to {} failed because network error will retry {}",
                        getRequest().getRemoteURI().getAddress(), retryTimes);
                    request(getRequest(), chainedCallback); // retry
                } else {
                    removeNetErrorRetryTimes(getRequest());
                    chainedCallback.onError(e);
                }
            } else {
                removeNetErrorRetryTimes(getRequest());
                chainedCallback.onError(e);
            }
        }
    }
}
