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
 
package com.baidu.cloud.starlight.api.model;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.utils.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by liuruisen on 2020/2/6.
 */
public class RpcRequest extends Request {

    private String serviceName;

    private String methodName;

    private Object[] params;

    private Class<?>[] paramsTypes;

    private Class<?> serviceClass;

    private Method method;

    private Object serviceObj;

    private ServiceConfig serviceConfig; // target service config, contains protocol/timeout

    private URI remoteURI;

    private Type[] genericParamsTypes;

    public RpcRequest() {
        super();
    }

    public RpcRequest(long id) {
        super(id);
    }

    @Override
    public String getServiceName() {
        if (!StringUtils.isBlank(serviceName)) {
            return serviceName;
        }

        serviceName = serviceConfig.serviceName(serviceClass);
        return serviceName;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Object[] getParams() {
        return this.params;
    }

    @Override
    public void setParams(Object[] params) {
        this.params = params;
    }

    @Override
    public Class<?>[] getParamsTypes() {
        return this.paramsTypes;
    }

    @Override
    public void setParamsTypes(Class<?>[] paramsTypes) {
        this.paramsTypes = paramsTypes;
    }

    @Override
    public Class<?> getServiceClass() {
        return this.serviceClass;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public void setMethod(Method targetMethod) {
        this.method = targetMethod;
    }

    @Override
    public ServiceConfig getServiceConfig() {
        return this.serviceConfig;
    }

    @Override
    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public Object getServiceObj() {
        return this.serviceObj;
    }

    @Override
    public void setServiceObj(Object serviceObj) {
        this.serviceObj = serviceObj;
    }

    @Override
    public URI getRemoteURI() {
        return remoteURI;
    }

    @Override
    public void setRemoteURI(URI remoteURI) {
        this.remoteURI = remoteURI;
    }

    @Override
    public Type[] getGenericParamsTypes() {
        return genericParamsTypes;
    }

    @Override
    public void setGenericParamsTypes(Type[] genericParamsTypes) {
        this.genericParamsTypes = genericParamsTypes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RpcRequest{");
        sb.append("serviceName='").append(serviceName).append('\'');
        sb.append(", methodName='").append(methodName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
