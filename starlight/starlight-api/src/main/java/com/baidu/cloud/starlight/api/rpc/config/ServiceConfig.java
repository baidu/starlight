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
 
package com.baidu.cloud.starlight.api.rpc.config;

import com.baidu.cloud.starlight.api.utils.StringUtils;

/**
 * Rpc Service config: Interface Use to config rpc service Created by liuruisen on 2020/2/24.
 */
public class ServiceConfig {

    /**
     * User-defined service id When using brpc for cross-language communication, the service name will be specified by
     * the user.
     */
    private String serviceId;

    /**
     * Support service dimension protocol, If this field is specified, only that protocol will be used Note: not used
     * currently
     */
    private String protocol;

    /**
     * Support service dimension compressType, if this field is specified, will use this compress type to compress
     * request bytes and response bytes. Note: not used currently
     */
    private String compressType;

    /**
     * Only used in brpc protocol currently
     *
     * "pb2-std": metadata proto2, body proto2 "pb2-java": meta proto2, body preserve null
     */
    private String serializeMode;

    /**
     * Support service dimension filters, if this field is specified, will use theses filters to filter request and
     * response. Note: not used currently
     */
    private String filters;

    /**
     * timeout for calling target method
     */
    private Integer invokeTimeoutMills;

    /**
     * When using stargate communication, you need to specify the group and version information. Default value is
     * "normal". Only used in stargate request.
     */
    private String group = "normal";

    /**
     * When using stargate communication, you need to specify the group and version information. Default value is
     * "1.0.0" Only used in stargate request.
     */
    private String version = "1.0.0";

    /*********** Server Side only: ThreadPool config ***********/
    private Integer threadPoolSize; // biz thread pool size

    private Integer maxThreadPoolSize;

    private Integer maxRunnableQueueSize;

    private Integer idleThreadKeepAliveSecond;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public Integer getInvokeTimeoutMills() {
        return invokeTimeoutMills;
    }

    public void setInvokeTimeoutMills(Integer invokeTimeoutMills) {
        this.invokeTimeoutMills = invokeTimeoutMills;
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public Integer getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public void setMaxThreadPoolSize(Integer maxThreadPoolSize) {
        this.maxThreadPoolSize = maxThreadPoolSize;
    }

    public Integer getMaxRunnableQueueSize() {
        return maxRunnableQueueSize;
    }

    public void setMaxRunnableQueueSize(Integer maxRunnableQueueSize) {
        this.maxRunnableQueueSize = maxRunnableQueueSize;
    }

    public Integer getIdleThreadKeepAliveSecond() {
        return idleThreadKeepAliveSecond;
    }

    public void setIdleThreadKeepAliveSecond(Integer idleThreadKeepAliveSecond) {
        this.idleThreadKeepAliveSecond = idleThreadKeepAliveSecond;
    }

    public String getCompressType() {
        return compressType;
    }

    public void setCompressType(String compressType) {
        this.compressType = compressType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSerializeMode() {
        return serializeMode;
    }

    public void setSerializeMode(String serializeMode) {
        this.serializeMode = serializeMode;
    }

    /**
     * Get unique ServiceName for target service. <1> serviceId </1> <2> interfaceName </2>
     * 
     * @param serviceClass
     * @return
     */
    public String serviceName(Class<?> serviceClass) {
        // Support cross-language requirements in Brpc scenarios: user can defined serviceName
        if (!StringUtils.isBlank(serviceId)) {
            return this.getServiceId();
        }

        return serviceClass.getName();
    }
}
