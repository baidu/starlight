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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.subcluster;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.protocol.stargate.StargateProtocol;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import com.baidu.cloud.starlight.springcloud.client.cluster.Cluster;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.OutlierConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.thirdparty.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by liuruisen on 2021/9/26.
 */
public class DefaultCluster implements Cluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCluster.class);

    private ClusterSelector clusterSelector;

    // NOTICE 支持修改lb后, 进行更改线程和使用线程的并发控制
    private LoadBalancer loadBalancer;

    private volatile Map<Class<?>, ServiceConfig> serviceRefers;

    // 客户端集群全局配置
    private StarlightClientProperties globalConfig;

    public DefaultCluster(ClusterSelector clusterSelector, StarlightClientProperties globalConfig,
        LoadBalancer loadBalancer) {
        this.clusterSelector = clusterSelector;
        this.serviceRefers = new ConcurrentHashMap<>();
        this.globalConfig = globalConfig;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void setServiceRefers(Map<Class<?>, ServiceConfig> referServices) {
        this.serviceRefers = referServices;
    }

    @Override
    public ClusterSelector getClusterSelector() {
        return clusterSelector;
    }

    @Override
    public void execute(Request request, RpcCallback callback) {
        // select instance
        long starTime = System.currentTimeMillis();
        ServiceInstance instance = getLoadBalancer().choose(clusterSelector);
        LOGGER.debug("Select instance from ribbon cost: {}", System.currentTimeMillis() - starTime);
        if (instance == null) {
            throw new StarlightRpcException(SpringCloudConstants.NO_INSTANCE_ERROR_CODE,
                "No instances available for service " + getClusterSelector().getServiceId() + ", cluster "
                    + getClusterSelector().getClusterName());
        }

        SingleStarlightClient starlightClient = null;
        try {
            starlightClient = initSingleClient(instance.getHost(), instance.getPort());
        } catch (Exception e) {
            // 有可能无法走到凤睛切面所在的逻辑，导致span信息里无remoteIp，这里设置上
            RpcContext.getContext().setRemoteAddress(instance.getHost(), instance.getPort());
            LOGGER.warn("AbstractClusterClient unexpect error ", e);
            callback.onError(e);
            return;
        }

        // request protocol
        ServiceConfig serviceConfig = request.getServiceConfig();
        if (serviceConfig == null) {
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "The request service has not been refer, please call refer() before request, " + "service {"
                    + request.getServiceClass().getName() + "}");
        }
        String protocolName = requestProtocol(serviceConfig, instance);
        request.setProtocolName(protocolName);

        // stargate group & version
        // Temporary plan for migration stargate
        addStargateMetadata(request, serviceConfig, instance);

        // execute request
        try {
            // 在此处执行调用主要为了兼容spring cloud的逻辑，不兼容是否可以；附带执行一些cluster特有逻辑
            getLoadBalancer().execute(clusterSelector, starlightClient, instance, request, callback);
        } catch (Throwable e) {
            // 有可能无法走到凤睛切面所在的逻辑，导致span信息里无remoteIp，这里设置上
            RpcContext.getContext().setRemoteAddress(instance.getHost(), instance.getPort());
            callback.onError(new StarlightRpcException(StarlightRpcException.INTERNAL_SERVER_ERROR,
                "Request failed: " + e.getMessage()));
        }
    }

    /**
     * Protocol selection When no protocol is specified, the protocol selection strategy will be adopted.
     */
    private String requestProtocol(ServiceConfig serviceConfig, ServiceInstance instance) {
        String protocolName = serviceConfig.getProtocol();
        if (StringUtils.isEmpty(protocolName)) {
            if (instance.getMetadata() == null || instance.getMetadata().size() == 0
                || StringUtils.isEmpty(instance.getMetadata().get(SpringCloudConstants.PROTOCOLS_KEY))) {
                LOGGER.warn("Unable to select protocol for request: "
                    + "there is nor protocol message in registration message or in configuration."
                    + "Will use the default protocol brpc");
                protocolName = Constants.BRPC_VALUE;
            } else {
                String protocols = instance.getMetadata().get(SpringCloudConstants.PROTOCOLS_KEY);
                protocolName = protocols.split(",")[0];
            }
        }

        return protocolName;
    }

    /**
     * when call stargate provider, request need to take version and group information
     */
    private void addStargateMetadata(Request request, ServiceConfig serviceConfig, ServiceInstance instance) {

        if (!request.getProtocolName().equals(StargateProtocol.PROTOCOL_NAME)) {
            return;
        }

        if (instance.getMetadata() == null || instance.getMetadata().size() <= 0) {
            return;
        }

        if (StringUtils.isEmpty(instance.getMetadata().get(SpringCloudConstants.INTERFACES_KEY))) {
            LOGGER.warn(
                "Request service {} method {} protocol stargate. "
                    + "There is no interfaces message in registration message, "
                    + "will use default group[normal] and version[1.0.0]",
                request.getServiceName(), request.getMethodName());
            return;
        }

        String interfaceStr = instance.getMetadata().get(SpringCloudConstants.INTERFACES_KEY);
        if (!interfaceStr.contains(request.getServiceClass().getName())) {
            LOGGER.warn(
                "Request service {}, method {}, protocol stargate, interface metadata {}. "
                    + "The registration interfaces metadata dose not contain the service, "
                    + "will use default group[normal] and version[1.0.0]. ",
                request.getServiceClass().getName(), request.getMethodName(), interfaceStr);
            return;
        }

        try {
            List<String> interfaces =
                JsonSerializer.OBJECT_MAPPER.readValue(interfaceStr, new TypeReference<List<String>>() {});

            if (interfaces == null || interfaces.size() == 0) {
                LOGGER.warn(
                    "Request service {}, method {}, protocol stargate, interface metadata {}. "
                        + "The result of parsing registration interfaces metadata is empty, "
                        + "will use default group[normal] and version[1.0.0]. ",
                    request.getServiceClass().getName(), request.getMethodName(), interfaceStr);
                return;
            }

            for (String interfaceName : interfaces) {
                if (interfaceName.contains(request.getServiceClass().getName())) {
                    String[] metadata = interfaceName.split(":");
                    if (metadata.length != 3) {
                        LOGGER.warn(
                            "Request service {}, method {}, protocol stargate, interface {}. "
                                + "The interface info parse from registration metadata is illegal, "
                                + "will use default group[normal] and version[1.0.0]. ",
                            request.getServiceClass().getName(), request.getMethodName(), interfaceName);
                        return;
                    }
                    serviceConfig.setGroup(metadata[0]);
                    serviceConfig.setVersion(metadata[2]);
                    LOGGER.debug("Request service {}, method {} use protocol {}, group is {}, version is {}",
                        request.getServiceName(), request.getMethodName(), request.getProtocolName(),
                        serviceConfig.getGroup(), serviceConfig.getVersion());
                    return;
                }
            }

            LOGGER.debug(
                "Request service {}, method {}, protocol stargate, interfaces {}. "
                    + "The interface list parse from metadata do not contain the request service, "
                    + "will use default group[normal] and version[1.0.0]. ",
                request.getServiceClass().getName(), request.getMethodName(), interfaces);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Cannot use stargate to send request, " + "parse interfaces metadata failed, interface metadata "
                    + interfaceStr + " please check provider registration metadata");
        }
    }

    /**
     * 可被继承，做些动态修改client配置的操作
     * 
     * @param host
     * @param port
     * @return
     */
    protected SingleStarlightClient initSingleClient(String host, Integer port) {
        TransportConfig transportConfig = globalConfig.transportConfig(clusterSelector.getServiceId());
        transportConfig.setAdditional(clientConfigMap());
        // init
        return SingleStarlightClientManager.getInstance().getOrCreateSingleClient(host, port, transportConfig,
            serviceRefers);
    }

    /**
     * 依据配置信息，生成uri中的attachment信息 可用于动态修改uri中的配置，并为后续流程生效
     * 
     * @return
     */
    protected Map<String, String> clientConfigMap() {
        Map<String, String> configAdd = new HashMap<>();
        OutlierConfig outlierConfig = globalConfig.getOutlierConfig(clusterSelector.getServiceId());
        if (outlierConfig != null) {
            configAdd.put(SpringCloudConstants.OUTLIER_DETECT_ENABLED_KEY, String.valueOf(outlierConfig.getEnabled()));
            configAdd.put(SpringCloudConstants.OUTLIER_DETECT_INTERVAL_KEY,
                String.valueOf(outlierConfig.getDetectInterval()));
            configAdd.put(SpringCloudConstants.OUTLIER_DETECT_MINI_REQUEST_NUM_KEY,
                String.valueOf(outlierConfig.getFailurePercentMinRequest()));
            configAdd.put(SpringCloudConstants.OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY,
                String.valueOf(outlierConfig.getFailurePercentThreshold()));

            if (outlierConfig.getFailureCountThreshold() != null) {
                configAdd.put(SpringCloudConstants.OUTLIER_DETECT_FAIL_COUNT_THRESHOLD_KEY,
                    String.valueOf(outlierConfig.getFailureCountThreshold()));
            }
        }

        return configAdd;
    }
}
