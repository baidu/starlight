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
 
package com.baidu.cloud.starlight.springcloud.client.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by liuruisen on 2020/2/25.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcProxy {

    /**
     * Service discovery name.
     * In spring cloud context is provider Spring Application Name.
     * A name must be specified for all proxy, whether or not a url is provider.
     *
     * @return
     */
    String name() default "";

    /**
     * A Real URL, contains of protocol IP port.
     * Example: "protocol://ip:port".
     * Higher priority than {@link #name()}.
     * The protocol is optional, default is "brpc"
     *
     * @return
     */
    String remoteUrl() default "";

    /**
     * Rpc protocol, default is "" will select protocol according to priority
     * Default priority: brpc &gt; stargate &gt; springrest
     * @return
     */
    String protocol() default "";

    /**
     * Rpc Service Id
     * Default is interface.getName()
     * Only if protocol value is "brpc", user can specify this value.
     * @return
     */
    String serviceId() default "";

    /**
     * Used for {@link org.springframework.beans.factory.annotation.Qualifier}
     *
     * @return
     */
    String qualifier() default "";

}
