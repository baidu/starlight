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
 
package com.baidu.cloud.starlight.springcloud.lifecycle;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baidu.cloud.starlight.springcloud.client.StarlightClientAutoConfiguration;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;

@Configuration
@AutoConfigureAfter(value = {StarlightClientAutoConfiguration.class})
@ConditionalOnBean(value = {SingleStarlightClientManager.class})
public class StarlightClientLifecycleAutoConfiguration {
    @Bean
    public StarlightClientLifecycle starlightClientLifecycle(SingleStarlightClientManager singleStarlightClientManager,
        ApplicationContext applicationContext) {
        return new StarlightClientLifecycle(singleStarlightClientManager, applicationContext);
    }
}
