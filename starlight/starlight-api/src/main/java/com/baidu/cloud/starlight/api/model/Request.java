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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Collection of information used in the request. Will be used to assemble requests of each protocol(brpc http
 * stargate...) Created by liuruisen on 2019/12/3.
 */
public abstract class Request extends AbstractMsgBase {

    /**
     * Remote uri of the server instance
     * 
     * @return
     */
    public abstract URI getRemoteURI();

    /**
     * Remote uri of the server instance
     * 
     * @param remoteURI
     */
    public abstract void setRemoteURI(URI remoteURI);

    /**
     * Service name of provider service, the value could be <1> ServiceId (brpc crossing-language or generic call) </1>
     * <2> interfaceName (default) </2> In order to support multi protocol, we unified the data model of serviceName as
     * group:interfaceName:version.
     * 
     * @return
     */
    public abstract String getServiceName();

    public abstract void setServiceName(String serviceName);

    public abstract String getMethodName();

    public abstract void setMethodName(String methodName);

    public abstract Object[] getParams();

    public abstract void setParams(Object[] params);

    public abstract Class<?>[] getParamsTypes();

    public abstract void setParamsTypes(Class<?>[] paramsTypes);

    public Request() {
        super();
    }

    public Request(long id) {
        super(id);
    }

    public abstract Class<?> getServiceClass();

    public abstract void setServiceClass(Class<?> serviceClass);

    public abstract Method getMethod();

    public abstract void setMethod(Method targetMethod);

    /**
     * IF we can know service obj before {@link com.baidu.cloud.starlight.api.rpc.ServiceRegistry#discover(String)}, use
     * service instances for reflection calls directly such as in HttpDecoder
     * 
     * @return
     */
    public abstract Object getServiceObj();

    public abstract void setServiceObj(Object serviceObj);

    /**
     * ServiceConfig represent the service metadata: protocol/version/group/serviceId, etc.
     * 
     * @return
     */
    public abstract ServiceConfig getServiceConfig();

    public abstract void setServiceConfig(ServiceConfig serviceConfig);

    public abstract Type[] getGenericParamsTypes();

    public abstract void setGenericParamsTypes(Type[] genericParamsTypes);
}
