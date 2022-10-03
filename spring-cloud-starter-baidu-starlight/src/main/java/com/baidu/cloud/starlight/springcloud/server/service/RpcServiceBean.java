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
 
package com.baidu.cloud.starlight.springcloud.server.service;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.server.annotation.RpcService;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * Created by liuruisen on 2019-05-23.
 */
public class RpcServiceBean implements InitializingBean {

    private StarlightServer starlightServer;

    private Object target;

    private Class<?> targetInterfaceClass;

    @Autowired
    private StarlightServerProperties properties;

    private RpcService annotation;

    private com.baidu.cloud.starlight.api.rpc.RpcService rpcService;

    @Override
    public void afterPropertiesSet() throws Exception {
        ServiceConfig serviceConfig = serviceConfig(annotation, properties);
        rpcService = new com.baidu.cloud.starlight.api.rpc.RpcService(targetInterfaceClass, target, serviceConfig);
        starlightServer.export(rpcService);
    }

    private ServiceConfig serviceConfig(RpcService annotation, StarlightServerProperties properties) {
        if (annotation == null && properties == null) {
            return null;
        }

        ServiceConfig serviceConfig = new ServiceConfig();
        if (properties != null) {
            serviceConfig.setFilters(properties.getFilters());
        }

        if (annotation != null) {
            if (!StringUtils.isEmpty(annotation.filters())) {
                serviceConfig.setFilters(annotation.filters());
            }
            if (!StringUtils.isEmpty(annotation.protocol())) {
                serviceConfig.setProtocol(annotation.protocol());
            }
            if (!StringUtils.isEmpty(annotation.serviceId())) {
                serviceConfig.setServiceId(annotation.serviceId());
            }
        }
        // filters
        String filters = serviceConfig.getFilters();
        if (filters != null && !filters.isEmpty()) {
            filters = SpringCloudConstants.DEFAULT_SERVER_FILTERS + Constants.FILTER_NAME_SPLIT_KEY + filters.trim();
        } else {
            filters = SpringCloudConstants.DEFAULT_SERVER_FILTERS;
        }
        serviceConfig.setFilters(filters);

        return serviceConfig;
    }

    public void setStarlightServer(StarlightServer starlightServer) {
        this.starlightServer = starlightServer;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setProperties(StarlightServerProperties properties) {
        this.properties = properties;
    }

    public void setTargetInterfaceClass(Class<?> targetInterfaceClass) {
        this.targetInterfaceClass = targetInterfaceClass;
    }

    public void setAnnotation(RpcService annotation) {
        this.annotation = annotation;
    }
}
