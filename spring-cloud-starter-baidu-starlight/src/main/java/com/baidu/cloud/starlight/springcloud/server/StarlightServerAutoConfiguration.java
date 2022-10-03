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
 
package com.baidu.cloud.starlight.springcloud.server;

import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightServer;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.server.annotation.RpcService;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by liuruisen on 2020/3/5.
 */
@Configuration
@ConditionalOnClass(RpcService.class)
@EnableConfigurationProperties(StarlightServerProperties.class)
@ConditionalOnProperty(value = "starlight.server.enable")
public class StarlightServerAutoConfiguration {

    @Bean(name = SpringCloudConstants.STARLIGHT_SERVER_NAME)
    public StarlightServer starlightServer(StarlightServerProperties serverProperties) {
        StarlightServer starlightServer = new DefaultStarlightServer(serverProperties.getHost(),
            ApplicationContextUtils.getServerPort(), serverProperties.transportConfig());
        starlightServer.init();
        starlightServer.serve();
        return starlightServer;
    }
}
