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
 
package com.baidu.cloud.starlight.springcloud.client;

import com.baidu.cloud.thirdparty.jackson.core.JsonProcessingException;
import com.baidu.cloud.thirdparty.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

/**
 * 初始化阶段存储本consumer的所有下游服务 不存储直连的remote url Created by liuruisen on 2021/12/5.
 */
public class RpcProxyProviders {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Set<String> providers = new HashSet<>();

    public Set<String> getProviders() {
        return providers;
    }

    public void setProviders(Set<String> providers) {
        this.providers = providers;
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
