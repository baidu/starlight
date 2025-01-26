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
 
package com.baidu.cloud.starlight.springcloud.server.register.gravity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.client.serviceregistry.Registration;

import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterListener;

/**
 * Created by liuruisen on 2020/8/10.
 */
public class StarlightGravityRegisterListener extends StarlightRegisterListener {

    @Override
    protected Registration createStarlightRegistration() {
        GravityRegistration registration = new GravityRegistration();
        registration.setServiceId(getAppName(applicationContext.getEnvironment()));
        registration.setPort(getPort(applicationContext.getEnvironment()));
        registration.setSchema(RPC_TYPE);

        // metadata
        Map<String, String> metadata = registration.getMetadata();
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        metadata.putAll(starlightMetas());
        registration.setMetadata(metadata);

        return registration;
    }

    @Override
    protected Registration updateStarlightRegistration(Registration oldRegistration, Integer port) {
        GravityRegistration newRegistration = new GravityRegistration();
        newRegistration.setPort(port);
        newRegistration.setServiceId(oldRegistration.getServiceId());
        newRegistration.setSchema(RPC_TYPE);

        // 不复用 oldRegistration 的 metadata,
        // 因为其中的 EPOCH 时间戳需要更新，Consumer 端根据时间戳判断 Provider 是否重启了
        Map<String, String> metadata = newRegistration.getMetadata();
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        metadata.putAll(starlightMetas());
        newRegistration.setMetadata(metadata);

        return newRegistration;
    }

}
