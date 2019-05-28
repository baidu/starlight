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
package com.baidu.brpc.spring.boot.autoconfigure.config;

import com.baidu.brpc.server.RpcServerOptions;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RpcServerConfig extends RpcServerOptions {
    private int port;
    private boolean useSharedThreadPool;
    private String interceptorBeanName;

    public RpcServerConfig() {
    }

    public RpcServerConfig(RpcServerConfig rhs) {
        super(rhs);
        this.port = rhs.getPort();
        this.useSharedThreadPool = rhs.isUseSharedThreadPool();
        this.interceptorBeanName = rhs.getInterceptorBeanName();
    }
}
