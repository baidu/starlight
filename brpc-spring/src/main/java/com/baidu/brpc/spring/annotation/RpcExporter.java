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
package com.baidu.brpc.spring.annotation;

import com.baidu.brpc.spring.RpcServiceExporter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation publish for {@link RpcServiceExporter}.
 *
 * @author xiemalin
 * @since 2.0.2
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RpcExporter {

    /**
     * RPC server port to publish.
     *
     * @return the string
     */
    String port() default "8080";

    /**
     * bean name of RPC server options bean type must be {@link com.baidu.brpc.server.RpcServerOptions}.
     *
     * @return the string
     */
    String rpcServerOptionsBeanName() default "";

    /**
     * bean name of RPC interceptor bean type must be {@link com.baidu.brpc.interceptor.Interceptor}.
     *
     * @return the string
     */
    String interceptorBeanName() default "";

    /**
     * Group for naming service
     */
    String group() default "normal";

    /**
     * Version for naming service
     */
    String version() default "1.0.0";

    /**
     * ignore it when failed to register naming service
     *
     * @return true, ignore
     */
    boolean ignoreFailOfNamingService() default false;

    /**
     * true: use the shared thread pool
     * false: create individual thread pool for register service
     */
    boolean useSharedThreadPool() default true;

    /**
     * Extra naming options. This option is effective on service-scope.
     * <p>
     * This config may have different behavior depending on which NamingService is used,
     * consult documentation of the specific {@link com.baidu.brpc.naming.NamingService} for detailed usage.
     */
    NamingOption[] extraOptions() default {};
}
