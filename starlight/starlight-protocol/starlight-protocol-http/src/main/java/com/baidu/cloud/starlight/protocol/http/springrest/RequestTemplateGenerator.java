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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.starlight.api.model.RpcRequest;
import feign.MethodMetadata;
import feign.RequestTemplate;

/**
 * Used to generate {@link RequestTemplate} when convert {@link RpcRequest} to Http request template. Composed of
 * {@link MethodMetadata} and {@link RequestTemplateArgsResolver}. Will cached in {@link SpringRestHttpEncoder}. Created
 * by liuruisen on 2020/6/3.
 */
public class RequestTemplateGenerator {

    /**
     * Method metadata info, include method info, target class, http request metadata. Used to generate
     * {@link RequestTemplate} and further generate Http Request.
     */
    private final MethodMetadata methodMetadata;

    /**
     * Use method parameters to improve {@link RequestTemplate}.
     */
    private final RequestTemplateArgsResolver argsResolver;

    public RequestTemplateGenerator(MethodMetadata methodMetadata, RequestTemplateArgsResolver argsResolver) {
        this.methodMetadata = methodMetadata;
        this.argsResolver = argsResolver;
    }

    public RequestTemplate create(Object[] args) {
        return argsResolver.create(args, methodMetadata);
    }
}
