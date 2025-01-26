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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.route;

import com.baidu.cloud.starlight.springcloud.configuration.ConfigChangeEvent;
import com.baidu.cloud.starlight.springcloud.configuration.ConfigListener;


/**
 * Created by liuruisen on 2021/10/25.
 */
public abstract class DynamicRouter extends AbstractRouter implements ConfigListener {


    @Override
    public void onConfigChange(ConfigChangeEvent event) {
        LOGGER.info("[XDS_ROUTE] Route config for {} changed {}", getServiceId(), event.getChangeType());
        try {
            Object configContent = event.getConfigContent();
            updateConfig(configContent);
        } catch (Throwable e) {
            LOGGER.warn("[XDS_ROUTE] Update dynamic route configs failed, serviceId{}, configEvent {}",
                    getServiceId(), event, e);
        }
    }

    /**
     * Update route config
     * 与route方法的执行间有同步问题，需加锁
     * @param routeConfig
     */
    public abstract void updateConfig(Object routeConfig);
}
