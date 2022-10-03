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
 
package com.baidu.cloud.starlight.api.rpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstraction of a rpc service Created by liuruisen on 2019/12/18.
 */
public class RpcService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcService.class);
    /**
     * The unique identifier of the service, the value could be <1> service Interface name </1> <2>
     * group:interfaceName:version </2>
     *
     * @return
     */
    private String serviceName;

    /**
     * The interface class of this service
     */
    private Class<?> serviceClass;

    /**
     * The object of service class
     */
    private Object serviceObj;

    /**
     * The config of this service: group version ...
     */
    private ServiceConfig serviceConfig;

    /**
     * Cache the target methods of this service
     */
    private Map<String, Method> methodMap;

    public RpcService(Class<?> serviceClass, Object serviceObj) {
        this.serviceClass = serviceClass;
        this.serviceObj = serviceObj;

        ServiceConfig config = new ServiceConfig();
        config.setFilters(Constants.DEFAULT_SERVER_FILTERS);
        this.serviceConfig = config;

        this.methodMap = new ConcurrentHashMap<>();
        cacheMethod();
    }

    public RpcService(Class<?> serviceClass, Object serviceObj, ServiceConfig serviceConfig) {
        this.serviceClass = serviceClass;
        this.serviceObj = serviceObj;
        this.serviceConfig = serviceConfig;
        this.methodMap = new ConcurrentHashMap<>();
        cacheMethod();
    }

    /**
     * ServiceKey Default value is service interface name
     *
     * @return
     */
    public String getServiceName() {
        if (!StringUtils.isBlank(serviceName)) {
            return serviceName;
        }
        serviceName = serviceConfig.serviceName(serviceClass);
        return serviceName;
    }

    /**
     * ServiceInterface
     *
     * @return
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * ServiceObject Implement of service interface
     *
     * @return
     */
    public Object getServiceObject() {
        return serviceObj;
    }

    /**
     * Get invoke method by methodKey
     *
     * @param methodName
     * @return
     */
    public Method getMethod(String methodName) {
        return methodMap.get(methodName);
    }

    /**
     * Get RpcService config
     *
     * @return
     */
    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    /**
     * Set RpcService config
     *
     * @param serviceConfig
     */
    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    private void cacheMethod() {
        Method[] methods = serviceClass.getMethods();
        for (Method method : methods) {
            if (isObjectNativeMethod(method)) {
                continue;
            }
            if (methodMap.get(method.getName()) != null) {
                throw new IllegalArgumentException(
                    "Starlight not support method overloading, {" + method.getName() + "}");
            }
            methodMap.put(method.getName(), method);
            LOGGER.debug("Cache RpcService {}, method {}", getServiceName(), method.getName());
        }
    }

    private boolean isObjectNativeMethod(Method method) {
        if (method.getDeclaringClass() == Object.class) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RpcService{");
        sb.append("serviceName='").append(serviceName).append('\'');
        sb.append(", serviceClass=").append(serviceClass);
        sb.append(", serviceObj=").append(serviceObj);
        sb.append(", serviceConfig=").append(serviceConfig);
        sb.append('}');
        return sb.toString();
    }
}
