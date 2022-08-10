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
package com.baidu.brpc.example.springboot.server;

import com.baidu.brpc.spring.annotation.NamingOption;
import com.baidu.brpc.spring.annotation.RpcExporter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Merged annotation for {@link RpcExporter}.
 *
 * @author zhangzicheng
 * @since 3.0.5
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@RpcExporter
public @interface MergedRpcExporter {
    
    /**
     * RPC server port to publish.
     *
     * @return the string
     */
    @AliasFor(annotation = RpcExporter.class)
    String port() default "8080";
    
    /**
     * bean name of RPC server options bean type must be {@link com.baidu.brpc.server.RpcServerOptions}.
     *
     * @return the string
     */
    @AliasFor(annotation = RpcExporter.class)
    String rpcServerOptionsBeanName() default "";
    
    /**
     * bean name of RPC interceptor bean type must be {@link com.baidu.brpc.interceptor.Interceptor}.
     *
     * @return the string
     */
    @AliasFor(annotation = RpcExporter.class)
    String interceptorBeanNames() default "";
    
    /**
     * Group for naming service
     */
    @AliasFor(annotation = RpcExporter.class)
    String group() default "normal";
    
    /**
     * Version for naming service
     */
    @AliasFor(annotation = RpcExporter.class)
    String version() default "1.0.0";
    
    /**
     * ignore it when failed to register naming service
     *
     * @return true, ignore
     */
    @AliasFor(annotation = RpcExporter.class)
    boolean ignoreFailOfNamingService() default false;
    
    /**
     * true: use the shared thread pool
     * false: create individual thread pool for register service
     * attention here - it is not global share thread pool between multi RpcClient/RpcServer , if you want to use
     * global thread pool , see rpc options.
     */
    @AliasFor(annotation = RpcExporter.class)
    boolean useServiceSharedThreadPool() default true;
    
    /**
     * Extra naming options. This option is effective on service-scope.
     * <p>
     * This config may have different behavior depending on which NamingService is used,
     * consult documentation of the specific {@link com.baidu.brpc.naming.NamingService} for detailed usage.
     */
    @AliasFor(annotation = RpcExporter.class)
    NamingOption[] extraOptions() default {};
}
