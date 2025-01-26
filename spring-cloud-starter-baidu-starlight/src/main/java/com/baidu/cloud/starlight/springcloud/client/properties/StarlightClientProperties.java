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
 
package com.baidu.cloud.starlight.springcloud.client.properties;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Global configuration for all RpcProxies Created by liuruisen on 2019-07-10.
 */
@ConfigurationProperties(prefix = StarlightClientProperties.PREFIX)
public class StarlightClientProperties {

    static final String PREFIX = "starlight.client";

    private String defaultConfig = "default"; // global level

    private Integer bizThreadNum; // global

    private Integer ioRatio; // global, netty ioRatio default 100

    private Map<String, ClientConfig> config; // support application level

    public Map<String, ClientConfig> getConfig() {
        return config;
    }

    public void setConfig(Map<String, ClientConfig> config) {
        this.config = config;
    }

    public ClientConfig getClientConfig(String clientName) {
        return config.get(clientName);
    }

    public String getDefaultConfig() {
        return defaultConfig;
    }

    public Integer getBizThreadNum() {
        return bizThreadNum;
    }

    public void setBizThreadNum(Integer bizThreadNum) {
        this.bizThreadNum = bizThreadNum;
    }

    public Integer getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(Integer ioRatio) {
        this.ioRatio = ioRatio;
    }

    public StarlightClientProperties() {
        this.config = new HashMap<>();
        config.putIfAbsent(defaultConfig, new ClientConfig());
    }

    public TransportConfig transportConfig(String name) {
        TransportConfig transportConfig = new TransportConfig();
        // config default
        transportConfig.setIoThreadNum(getIoThreadNum(name));
        transportConfig.setConnectTimeoutMills(getConnectTimeoutMills(name));
        transportConfig.setMaxHeartbeatTimes(getMaxHeartbeatTimes(name));
        transportConfig.setWriteTimeoutMills(getWriteTimeoutMills(name));
        transportConfig.setReadIdleTimeout(getReadIdleTimeout(name));
        transportConfig.setRequestTimeoutMills(getRequestTimeoutMills(name));

        // channel info
        transportConfig.setChannelType(getChannelType(name));
        transportConfig.setMaxConnections(getMaxConnections(name));
        transportConfig.setMaxIdleConnections(getMaxIdleConnections(name));
        transportConfig.setMinIdleConnections(getMinIdleConnections(name));
        transportConfig.setConnectKeepAliveEnable(getConnectKeepAliveEnable(name));

        transportConfig.setBizWorkThreadNum(this.bizThreadNum);
        transportConfig.setIoRatio(this.ioRatio);

        return transportConfig;
    }

    public Integer getIoThreadNum(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getIoThreadNum() != null) {
            return clientConfig.getIoThreadNum();
        }

        if (globalConfig != null && globalConfig.getIoThreadNum() != null) {
            return globalConfig.getIoThreadNum();
        }

        return Constants.DEFAULT_IO_THREADS_VALUE;
    }

    public Integer getConnectTimeoutMills(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getConnectTimeoutMills() != null) {
            return clientConfig.getConnectTimeoutMills();
        }

        if (globalConfig != null && globalConfig.getConnectTimeoutMills() != null) {
            return globalConfig.getConnectTimeoutMills();
        }

        return Constants.CONNECT_TIMEOUT_VALUE;
    }

    public Integer getMaxHeartbeatTimes(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getMaxHeartbeatTimes() != null) {
            return clientConfig.getMaxHeartbeatTimes();
        }

        if (globalConfig != null && globalConfig.getMaxHeartbeatTimes() != null) {
            return globalConfig.getMaxHeartbeatTimes();
        }

        return Constants.MAX_HEARTBEAT_TIMES_VALUE;
    }

    public Integer getWriteTimeoutMills(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getWriteTimeoutMills() != null) {
            return clientConfig.getWriteTimeoutMills();
        }

        if (globalConfig != null && globalConfig.getWriteTimeoutMills() != null) {
            return globalConfig.getWriteTimeoutMills();
        }

        return Constants.WRITE_TIMEOUT_VALUE;
    }

    public Integer getReadIdleTimeout(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getReadIdleTimeout() != null) {
            return clientConfig.getReadIdleTimeout();
        }

        if (globalConfig != null && globalConfig.getReadIdleTimeout() != null) {
            return globalConfig.getReadIdleTimeout();
        }

        return Constants.READ_IDLE_TIMEOUT_VALUE;
    }

    public Integer getRequestTimeoutMills(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getRequestTimeoutMills() != null) {
            return clientConfig.getRequestTimeoutMills();
        }

        if (globalConfig != null && globalConfig.getRequestTimeoutMills() != null) {
            return globalConfig.getRequestTimeoutMills();
        }

        return Constants.REQUEST_TIMEOUT_VALUE;
    }

    public Integer getRequestTimeoutMills(String clientName, String className) {
        ClientConfig clientConfig = config.get(clientName);
        if (clientConfig != null && clientConfig.getRequestTimeoutMills(className) != null) {
            return clientConfig.getRequestTimeoutMills(className);
        }

        ClientConfig globalConfig = config.get(defaultConfig);
        if (globalConfig != null && globalConfig.getRequestTimeoutMills(className) != null) {
            return globalConfig.getRequestTimeoutMills(className);
        }

        return Constants.REQUEST_TIMEOUT_VALUE;
    }

    public Integer getWarmUpRatio(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getWarmUpRatio() != null) {
            return clientConfig.getWarmUpRatio();
        }

        if (globalConfig != null && globalConfig.getWarmUpRatio() != null) {
            return globalConfig.getWarmUpRatio();
        }

        return SpringCloudConstants.DEFAULT_WARM_UP_RATIO;
    }

    public Integer getWarmUpCount(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getWarmUpCount() != null) {
            return clientConfig.getWarmUpCount();
        }

        if (globalConfig != null && globalConfig.getWarmUpCount() != null) {
            return globalConfig.getWarmUpCount();
        }

        return null;
    }

    public String getChannelType(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getChannelType() != null) {
            return clientConfig.getChannelType();
        }

        if (globalConfig != null && globalConfig.getChannelType() != null) {
            return globalConfig.getChannelType();
        }

        return Constants.DEFAULT_RPC_CHANNEL_TYPE_VALUE;
    }

    public String getClusterModel(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getClusterModel() != null) {
            return clientConfig.getClusterModel();
        }

        if (globalConfig != null && globalConfig.getClusterModel() != null) {
            return globalConfig.getClusterModel();
        }

        return SpringCloudConstants.DEFAULT_CLUSTER_MODEL;
    }

    public Integer getMaxConnections(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getMaxConnections() != null) {
            return clientConfig.getMaxConnections();
        }

        if (globalConfig != null && globalConfig.getMaxConnections() != null) {
            return globalConfig.getMaxConnections();
        }

        return Constants.MAX_TOTAL_CONNECTIONS;
    }

    public Integer getMaxIdleConnections(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getMaxIdleConnections() != null) {
            return clientConfig.getMaxIdleConnections();
        }

        if (globalConfig != null && globalConfig.getMaxIdleConnections() != null) {
            return globalConfig.getMaxIdleConnections();
        }

        return Constants.MAX_IDLE_CONNECTIONS;
    }

    public Integer getMinIdleConnections(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getMinIdleConnections() != null) {
            return clientConfig.getMinIdleConnections();
        }

        if (globalConfig != null && globalConfig.getMinIdleConnections() != null) {
            return globalConfig.getMinIdleConnections();
        }

        return Constants.MIN_IDLE_CONNECTIONS;
    }

    public String getProtocol(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getProtocol() != null) {
            return clientConfig.getProtocol();
        }

        if (globalConfig != null && globalConfig.getProtocol() != null) {
            return globalConfig.getProtocol();
        }

        return null; // protocol select logic will select a protocol
    }

    public String getCompressType(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getCompressType() != null) {
            return clientConfig.getCompressType();
        }

        if (globalConfig != null && globalConfig.getCompressType() != null) {
            return globalConfig.getCompressType();
        }

        return Constants.COMPRESS_TYPE;
    }

    public String getFilters(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getFilters() != null) {
            return SpringCloudConstants.DEFAULT_CLIENT_FILTERS + Constants.FILTER_NAME_SPLIT_KEY
                + clientConfig.getFilters();
        }

        if (globalConfig != null && globalConfig.getFilters() != null) {
            return SpringCloudConstants.DEFAULT_CLIENT_FILTERS + Constants.FILTER_NAME_SPLIT_KEY
                + globalConfig.getFilters();
        }

        return SpringCloudConstants.DEFAULT_CLIENT_FILTERS;
    }

    public Integer getRetryTimes(String clientName, String className) {
        ClientConfig clientConfig = config.get(clientName);
        if (clientConfig != null && clientConfig.getRetryTimes(className) != null) {
            return clientConfig.getRetryTimes(className);
        }

        ClientConfig globalConfig = config.get(defaultConfig);
        if (globalConfig != null && globalConfig.getRetryTimes(className) != null) {
            return globalConfig.getRetryTimes(className);
        }

        return SpringCloudConstants.DEFAULT_RETRY_TIMES;
    }

    public Integer getRetryDelayTimeUnitMills(String clientName, String className) {
        ClientConfig clientConfig = config.get(clientName);
        if (clientConfig != null && clientConfig.getRetryDelayTimeUnitMills(className) != null) {
            return clientConfig.getRetryDelayTimeUnitMills(className);
        }

        ClientConfig globalConfig = config.get(defaultConfig);
        if (globalConfig != null && globalConfig.getRetryDelayTimeUnitMills(className) != null) {
            return globalConfig.getRetryDelayTimeUnitMills(className);
        }

        return SpringCloudConstants.DEFAULT_RETRY_DELAY_MILLS;
    }

    public String getRetryMethods(String clientName, String className) {
        ClientConfig clientConfig = config.get(clientName);
        if (clientConfig != null && clientConfig.getRetryMethods(className) != null) {
            return clientConfig.getRetryMethods(className);
        }

        ClientConfig globalConfig = config.get(defaultConfig);
        if (globalConfig != null && globalConfig.getRetryMethods(className) != null) {
            return globalConfig.getRetryMethods(className);
        }

        return null;
    }

    public String getRetryErrorCodes(String clientName, String className) {
        ClientConfig clientConfig = config.get(clientName);
        if (clientConfig != null && clientConfig.getRetryErrorCodes(className) != null) {
            return clientConfig.getRetryErrorCodes(className);
        }

        ClientConfig globalConfig = config.get(defaultConfig);
        if (globalConfig != null && globalConfig.getRetryErrorCodes(className) != null) {
            return globalConfig.getRetryErrorCodes(className);
        }

        return null;
    }

    public String getSerializeMode(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getSerializeMode() != null) {
            return clientConfig.getSerializeMode();
        }

        if (globalConfig != null && globalConfig.getSerializeMode() != null) {
            return globalConfig.getSerializeMode();
        }

        return Constants.PROTO2_STD_MODE; // default is proto2 std: proto2-std
    }

    public Boolean getConnectKeepAliveEnable(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getConnectKeepAliveEnable() != null) {
            return clientConfig.getConnectKeepAliveEnable();
        }

        if (globalConfig != null && globalConfig.getConnectKeepAliveEnable() != null) {
            return globalConfig.getConnectKeepAliveEnable();
        }

        return Constants.CONNECT_KEEPALIVE_ENABLED_VALUE; // default is false
    }

    /**
     * Get outlier config for the specificed client
     * 
     * @param clientName
     * @return
     */
    public OutlierConfig getOutlierConfig(String clientName) {
        OutlierConfig outlierConfig = new OutlierConfig();
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        OutlierConfig globalOutlierConfig = null;
        if (globalConfig != null && globalConfig.getOutlier() != null) {
            globalOutlierConfig = globalConfig.getOutlier();
        }
        // default > new
        outlierConfig.merge(globalOutlierConfig);

        OutlierConfig clientOutlierConfig = null;
        if (clientConfig != null && clientConfig.getOutlier() != null) {
            clientOutlierConfig = clientConfig.getOutlier();
        }

        // client > default
        outlierConfig.merge(clientOutlierConfig);

        // set default value if the value is null
        if (outlierConfig.getEnabled() == null) {
            outlierConfig.setEnabled(SpringCloudConstants.OUTLIER_DETECT_ENABLED); // false
        }

        if (outlierConfig.getDetectInterval() == null) {
            // 30
            outlierConfig.setDetectInterval(SpringCloudConstants.OUTLIER_DETECT_INTERVAL);
        }

        if (outlierConfig.getFailurePercentMinRequest() == null) {
            // 5
            outlierConfig.setFailurePercentMinRequest(SpringCloudConstants.OUTLIER_DETECT_MINI_REQUEST_NUM);
        }

        if (outlierConfig.getFailurePercentThreshold() == null) {
            // 20%
            outlierConfig.setFailurePercentThreshold(SpringCloudConstants.OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD);
        }

        if (outlierConfig.getBaseEjectTime() == null) {
            // 30s
            outlierConfig.setBaseEjectTime(SpringCloudConstants.OUTLIER_DETECT_BASE_EJECT_TIME);
        }

        if (outlierConfig.getMaxEjectTime() == null) {
            // 300s
            outlierConfig.setMaxEjectTime(SpringCloudConstants.OUTLIER_DETECT_MAX_EJECT_TIME);
        }

        if (outlierConfig.getMaxEjectPercent() == null) {
            // 20%
            outlierConfig.setMaxEjectPercent(SpringCloudConstants.OUTLIER_DETECT_MAX_EJECT_PERCENT);
        }

        if (outlierConfig.getRecoverByCheckEnabled() == null) {
            // true
            outlierConfig.setRecoverByCheckEnabled(SpringCloudConstants.OUTLIER_RECOVER_BY_CHECK_ENABLED);
        }

        return outlierConfig;
    }

    public Integer getNetworkErrorRetryTimes(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getNetworkErrorRetryTimes() != null) {
            return clientConfig.getNetworkErrorRetryTimes();
        }

        if (globalConfig != null && globalConfig.getNetworkErrorRetryTimes() != null) {
            return globalConfig.getNetworkErrorRetryTimes();
        }

        return SpringCloudConstants.NETWORK_ERROR_RETRY_TIMES; // default is 3
    }

    public Boolean getLocalCacheEnabled(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getLocalCacheEnabled() != null) {
            return clientConfig.getLocalCacheEnabled();
        }

        if (globalConfig != null && globalConfig.getLocalCacheEnabled() != null) {
            return globalConfig.getLocalCacheEnabled();
        }

        return SpringCloudConstants.LOCAL_CACHE_ENABLED; // default value is true
    }

    public Boolean getStoreLocalCacheAsync(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getStoreLocalCacheAsync() != null) {
            return clientConfig.getStoreLocalCacheAsync();
        }

        if (globalConfig != null && globalConfig.getStoreLocalCacheAsync() != null) {
            return globalConfig.getStoreLocalCacheAsync();
        }

        return SpringCloudConstants.STORE_LOCAL_CACHE_ASYNC; // default value is false
    }

    public Boolean getFetchInstancesWhenInitEnabled(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getFetchInstancesWhenInitEnabled() != null) {
            return clientConfig.getFetchInstancesWhenInitEnabled();
        }

        if (globalConfig != null && globalConfig.getFetchInstancesWhenInitEnabled() != null) {
            return globalConfig.getFetchInstancesWhenInitEnabled();
        }

        return true; // default value is true
    }

    public Boolean getWarmUpEnabled(String clientName) {
        ClientConfig globalConfig = config.get(defaultConfig);
        ClientConfig clientConfig = config.get(clientName);

        if (clientConfig != null && clientConfig.getWarmUpEnabled() != null) {
            return clientConfig.getWarmUpEnabled();
        }

        if (globalConfig != null && globalConfig.getWarmUpEnabled() != null) {
            return globalConfig.getWarmUpEnabled();
        }

        return true; // default value is true
    }
}
