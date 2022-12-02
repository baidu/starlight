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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.netflix.loadbalancer.Server;

import java.util.Map;

/**
 * 统一各种注册中心starter的数据模型，如gravity consul nacos 注：仅支持基于spring cloud 2.0.x版本的注册中心starter（含ribbon依赖） 接入新版的Spring Cloud
 * loadbalancer后，可使用如下思路 1. 剔除Ribbon依赖，直接使用ServiceInstance接口定义(推荐) 2. 定义专属starlight的ServiceInstance作为接口定义
 *
 * @author liuruisen
 */
public class StarlightRibbonServer extends Server {

    private Map<String, String> metadata;

    public StarlightRibbonServer(String host, int port) {
        super("rpc", host, port);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
