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

import com.baidu.cloud.starlight.api.model.Request;

/**
 *
 * Created by liuruisen on 2021/9/6.
 */
public interface Router extends Comparable<Router> {

    /**
     * Route and return subset cluster
     * 
     * @param request
     * @return subset cluster
     */
    Cluster route(Request request);

    /**
     * 每个Router绑定一个serviceId，或者是元信息？
     * 
     * @return
     */
    default String getServiceId() {
        return null;
    }

    /**
     * 获取路由用的serviceId，用于支持跨注册中心路由场景
     * 
     * @return
     */
    String getRouteServiceId();

    /**
     * 路由优先级，从高优先级到低优先级匹配，匹配到一个Router后返回
     * 
     * @return
     */
    int getPriority();
}
