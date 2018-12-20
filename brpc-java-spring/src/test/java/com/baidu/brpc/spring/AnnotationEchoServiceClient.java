/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.brpc.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import com.baidu.brpc.spring.annotation.RpcProxy;

/**
 * Test class for {@link RpcProxy}
 * 
 * @author xiemalin
 * @since 2.17
 */
@Service("echoServiceClient")
@Getter
@Setter
public class AnnotationEchoServiceClient {

    @RpcProxy(serviceUrl = "list://127.0.0.1:8012",
            lookupStubOnStartup = false,
            rpcClientOptionsBeanName = "rpcClientOptions",
            interceptorBeanName = "customInterceptor")
    private EchoService echoService;
}
