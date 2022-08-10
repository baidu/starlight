/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.example.springboot.client;

import com.baidu.brpc.spring.annotation.RpcProxy;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation publish for {@link RpcProxy}.
 *
 * @author zhangzicheng
 * @since 3.0.5
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@RpcProxy
public @interface MergedRpcProxy {
    
    /**
     * bean name of RPC client options.  bean type must be {@link com.baidu.brpc.client.RpcClientOptions}
     *
     * @return the string
     */
    @AliasFor(annotation = RpcProxy.class)
    String rpcClientOptionsBeanName() default "";
    
    /**
     * RPC server naming url to connect.
     *
     * @return the string
     */
    @AliasFor(annotation = RpcProxy.class)
    String namingServiceUrl() default "";
    
    /**
     * group for naming service
     */
    @AliasFor(annotation = RpcProxy.class)
    String group() default "normal";
    
    /**
     * version for naming service
     */
    @AliasFor(annotation = RpcProxy.class)
    String version() default "1.0.0";
    
    /**
     * ignore it when failed to lookup/subscribe naming service
     *
     * @return true, ignore
     */
    @AliasFor(annotation = RpcProxy.class)
    boolean ignoreFailOfNamingService() default false;
    
    /**
     * bean name of RPC interceptor bean type must be {@link com.baidu.brpc.interceptor.Interceptor}.
     *
     * @return the string
     */
    @AliasFor(annotation = RpcProxy.class)
    String interceptorBeanNames() default "";
    
    /**
     * use name to identify all the instances for this service from registry.
     */
    @AliasFor(annotation = RpcProxy.class)
    String name() default "";
}
