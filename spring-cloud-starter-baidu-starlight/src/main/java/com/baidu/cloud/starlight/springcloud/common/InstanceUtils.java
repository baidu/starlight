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
 
package com.baidu.cloud.starlight.springcloud.common;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

/**
 * Created by liuruisen on 2020/12/2.
 */
public class InstanceUtils {

    public static String ipPortStr(String host, Integer port) {
        return String.format("%s:%d", host, port);
    }

    public static String ipPortStr(ServiceInstance serviceInstance) {
        return ipPortStr(serviceInstance.getHost(), serviceInstance.getPort());
    }

    /**
     * 是否是bns实例
     * @param serviceId
     * @return
     */
    public static boolean isBnsServiceId(String serviceId){
        if (StringUtils.isEmpty(serviceId)){
            return false;
        }

        String[] parts = serviceId.split("\\.");
        return parts.length == 4;
    }

    /**
     * 是否是gravity实例
     * @param serviceId
     * @return
     */
    public static boolean isGravityServiceId(String serviceId) {
        if (StringUtils.isEmpty(serviceId)) {
            return false;
        }

        String[] parts = serviceId.split("\\.");
        return parts.length != 4;
    }
}
