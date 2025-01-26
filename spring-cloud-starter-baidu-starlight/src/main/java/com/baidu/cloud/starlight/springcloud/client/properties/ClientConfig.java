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

import java.util.HashMap;
import java.util.Map;

/**
 * application level config
 */
public class ClientConfig {

    private String protocol;

    private String compressType; // default none

    private String filters; // split by ","

    private String clusterModel; // default SpringCloudConstants.DEFAULT_CLUSTER_MODEL application level

    private Boolean warmUpEnabled;

    // warm up ratio, used to warm up ClusterStarlightClient
    private Integer warmUpRatio; // default SpringCloudConstants.DEFAULT_WARM_UP_RATIO

    private Integer warmUpCount; // same as warmUpRatio, prior than warmUpRatio

    private Integer connectTimeoutMills;

    private Integer writeTimeoutMills; // default Constants.WRITE_TIMEOUT_VALUE;

    private Integer requestTimeoutMills; // default Constants.REQUEST_TIMEOUT_VALUE;

    private Integer readIdleTimeout; // default Constants.READ_IDLE_TIMEOUT_VALUE; // second

    private Integer maxHeartbeatTimes; // default Constants.MAX_HEARTBEAT_TIMES_VALUE;

    private Integer ioThreadNum;

    private String channelType; // default Constants.DEFAULT_RPC_CHANNEL_TYPE_VALUE;

    private Integer maxConnections;

    private Integer maxIdleConnections;

    private Integer minIdleConnections;

    private Integer retryTimes; // application level

    private String retryMethods; // application level

    private String retryErrorCodes; // split by ,

    /**
     * The time unit of the time interval between two retries,
     * which will be used to calculate the true delay time.
     */
    private Integer retryDelayTimeUnitMills; // application level

    /**
     * Only used in brpc protocol currently
     *
     * "pb2-std": metadata proto2, body proto2
     * "pb2-java": meta proto2, body preserve null
     */
    private String serializeMode;

    /**
     * Whether to enable connection keep-alive capability.
     * Default is false
     */
    private Boolean connectKeepAliveEnable;

    private Integer networkErrorRetryTimes;

    /**
     * The config of outlier detect and eject
     */
    private OutlierConfig outlier;

    private Boolean localCacheEnabled;

    private Boolean storeLocalCacheAsync;

    /**
     * for hairuo， 控制程序初始化阶段是否可以拉取服务列表
     */
    private Boolean fetchInstancesWhenInitEnabled;

    /**
     * support interface level
     */
    private Map<String, InterfaceConfig> interfaceConfig;

    public ClientConfig() {
        this.interfaceConfig = new HashMap<>();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCompressType() {
        return compressType;
    }

    public void setCompressType(String compressType) {
        this.compressType = compressType;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public String getClusterModel() {
        return clusterModel;
    }

    public void setClusterModel(String clusterModel) {
        this.clusterModel = clusterModel;
    }

    public Integer getWarmUpRatio() {
        return warmUpRatio;
    }

    public void setWarmUpRatio(Integer warmUpRatio) {
        this.warmUpRatio = warmUpRatio;
    }

    public Integer getWarmUpCount() {
        return warmUpCount;
    }

    public void setWarmUpCount(Integer warmUpCount) {
        this.warmUpCount = warmUpCount;
    }

    public Integer getConnectTimeoutMills() {
        return connectTimeoutMills;
    }

    public void setConnectTimeoutMills(Integer connectTimeoutMills) {
        this.connectTimeoutMills = connectTimeoutMills;
    }

    public Integer getWriteTimeoutMills() {
        return writeTimeoutMills;
    }

    public void setWriteTimeoutMills(Integer writeTimeoutMills) {
        this.writeTimeoutMills = writeTimeoutMills;
    }

    public Integer getReadIdleTimeout() {
        return readIdleTimeout;
    }

    public void setReadIdleTimeout(Integer readIdleTimeout) {
        this.readIdleTimeout = readIdleTimeout;
    }

    public Integer getMaxHeartbeatTimes() {
        return maxHeartbeatTimes;
    }

    public void setMaxHeartbeatTimes(Integer maxHeartbeatTimes) {
        this.maxHeartbeatTimes = maxHeartbeatTimes;
    }

    public Integer getIoThreadNum() {
        return ioThreadNum;
    }

    public void setIoThreadNum(Integer ioThreadNum) {
        this.ioThreadNum = ioThreadNum;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(Integer maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public Integer getMinIdleConnections() {
        return minIdleConnections;
    }

    public void setMinIdleConnections(Integer minIdleConnections) {
        this.minIdleConnections = minIdleConnections;
    }

    public Integer getRequestTimeoutMills() {
        return requestTimeoutMills;
    }

    public Integer getRequestTimeoutMills(String className) {
        InterfaceConfig config = interfaceConfig.get(className);
        if (config != null && config.getRequestTimeoutMills() != null) {
            return config.getRequestTimeoutMills();
        }

        if (requestTimeoutMills != null) {
            return requestTimeoutMills;
        }

        return null;
    }

    public void setRequestTimeoutMills(Integer requestTimeoutMills) {
        this.requestTimeoutMills = requestTimeoutMills;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    /**
     * Get retry times for the specify interface
     * 
     * @param className
     * @return
     */
    public Integer getRetryTimes(String className) {
        InterfaceConfig config = interfaceConfig.get(className);
        if (config != null && config.getRetryTimes() != null) {
            return config.getRetryTimes();
        }

        if (retryTimes != null) {
            return retryTimes;
        }

        return null;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public String getRetryMethods() {
        return retryMethods;
    }

    /**
     * Get retry methods for the specify interface
     * 
     * @param className
     * @return
     */
    public String getRetryMethods(String className) {
        InterfaceConfig config = interfaceConfig.get(className);
        if (config != null && config.getRetryMethods() != null) {
            return config.getRetryMethods();
        }

        if (retryMethods != null) {
            return retryMethods;
        }

        return null;
    }

    public void setRetryMethods(String retryMethods) {
        this.retryMethods = retryMethods;
    }

    public Integer getRetryDelayTimeUnitMills() {
        return retryDelayTimeUnitMills;
    }

    /**
     * Get retry retryDelayTimeUnitMills for the specify interface
     * 
     * @param className
     * @return
     */
    public Integer getRetryDelayTimeUnitMills(String className) {
        InterfaceConfig config = interfaceConfig.get(className);
        if (config != null && config.getRetryDelayTimeUnitMills() != null) {
            return config.getRetryDelayTimeUnitMills();
        }

        if (retryDelayTimeUnitMills != null) {
            return retryDelayTimeUnitMills;
        }

        return null;
    }

    public void setRetryDelayTimeUnitMills(Integer retryDelayTimeUnitMills) {
        this.retryDelayTimeUnitMills = retryDelayTimeUnitMills;
    }

    public Map<String, InterfaceConfig> getInterfaceConfig() {
        return interfaceConfig;
    }

    public void setInterfaceConfig(Map<String, InterfaceConfig> interfaceConfig) {
        this.interfaceConfig = interfaceConfig;
    }

    public String getSerializeMode() {
        return serializeMode;
    }

    public void setSerializeMode(String serializeMode) {
        this.serializeMode = serializeMode;
    }

    public Boolean getConnectKeepAliveEnable() {
        return connectKeepAliveEnable;
    }

    public void setConnectKeepAliveEnable(Boolean connectKeepAliveEnable) {
        this.connectKeepAliveEnable = connectKeepAliveEnable;
    }

    public OutlierConfig getOutlier() {
        return outlier;
    }

    public void setOutlier(OutlierConfig outlier) {
        this.outlier = outlier;
    }

    public Integer getNetworkErrorRetryTimes() {
        return networkErrorRetryTimes;
    }

    public void setNetworkErrorRetryTimes(Integer networkErrorRetryTimes) {
        this.networkErrorRetryTimes = networkErrorRetryTimes;
    }

    public Boolean getLocalCacheEnabled() {
        return localCacheEnabled;
    }

    public void setLocalCacheEnabled(Boolean localCacheEnabled) {
        this.localCacheEnabled = localCacheEnabled;
    }

    public Boolean getStoreLocalCacheAsync() {
        return storeLocalCacheAsync;
    }

    public void setStoreLocalCacheAsync(Boolean storeLocalCacheAsync) {
        this.storeLocalCacheAsync = storeLocalCacheAsync;
    }

    public Boolean getFetchInstancesWhenInitEnabled() {
        return fetchInstancesWhenInitEnabled;
    }

    public void setFetchInstancesWhenInitEnabled(Boolean fetchInstancesWhenInitEnabled) {
        this.fetchInstancesWhenInitEnabled = fetchInstancesWhenInitEnabled;
    }

    public Boolean getWarmUpEnabled() {
        return warmUpEnabled;
    }

    public void setWarmUpEnabled(Boolean warmUpEnabled) {
        this.warmUpEnabled = warmUpEnabled;
    }

    public String getRetryErrorCodes() {
        return retryErrorCodes;
    }

    /**
     * Get retry methods for the specify interface
     * 
     * @param className
     * @return
     */
    public String getRetryErrorCodes(String className) {
        InterfaceConfig config = interfaceConfig.get(className);
        if (config != null && config.getRetryErrorCodes() != null) {
            return config.getRetryErrorCodes();
        }

        if (retryErrorCodes != null) {
            return retryErrorCodes;
        }

        return null;
    }

    public void setRetryErrorCodes(String retryErrorCodes) {
        this.retryErrorCodes = retryErrorCodes;
    }
}