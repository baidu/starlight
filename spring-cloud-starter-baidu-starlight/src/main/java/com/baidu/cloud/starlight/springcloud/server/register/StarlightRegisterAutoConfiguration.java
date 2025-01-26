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
 
package com.baidu.cloud.starlight.springcloud.server.register;

import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.springcloud.server.register.consul.StarlightConsulRegisterListener;
import com.baidu.cloud.starlight.springcloud.server.register.gravity.StarlightGravityRegisterListener;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
@AutoConfigureAfter(name = {"com.baidu.cloud.starlight.springcloud.server.StarlightServerAutoConfiguration",
        "org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistryAutoConfiguration",
        "com.baidu.formula.discovery.serviceregistry.FormulaServiceRegistryAutoConfiguration",
        "com.baidu.cloud.gravity.discovery.registry.GravityAutoServiceRegistrationAutoConfiguration"})
@ConditionalOnClass(name = "org.springframework.cloud.client.discovery.DiscoveryClient")
@ConditionalOnProperty(name = "spring.cloud.discovery.enabled", matchIfMissing = true)
@ConditionalOnBean(StarlightServer.class)
public class StarlightRegisterAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name =
            "com.baidu.cloud.gravity.discovery.registry.GravityAutoServiceRegistrationAutoConfiguration")
    protected static class StarlightGravityRegisterAutoConfiguration {

        @Bean(destroyMethod = "destroy")
        @ConditionalOnMissingBean
        @ConditionalOnBean(type =
                "com.baidu.cloud.gravity.discovery.registry.GravityAutoServiceRegistrationAutoConfiguration")
        public StarlightGravityRegisterListener starlightGravityRegisterListener() {
            return new StarlightGravityRegisterListener();
        }
    }

    @Configuration
    @ConditionalOnClass(ConsulServiceRegistryAutoConfiguration.class)
    protected static class StarlightConsulRegisterAutoConfiguration {

        @Bean(destroyMethod = "destroy")
        @ConditionalOnMissingBean
        @ConditionalOnBean(ConsulServiceRegistryAutoConfiguration.class)
        public StarlightRegisterListener consulRegisterListener() {
            return new StarlightConsulRegisterListener();
        }
    }

}
