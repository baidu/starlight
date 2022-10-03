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
 
package com.baidu.cloud.starlight.springcloud.common;

import org.springframework.util.ClassUtils;

/**
 * Created by liuruisen on 2021/10/21.
 */
public class ClassPresentUtils {

    private static final String GRAVITY_DISCOVERY_SERVER_CLASS =
        "com.baidu.cloud.gravity.discovery.discovery.ribbon.GravityDiscoveryServer";

    private static final boolean GRAVITY_DISCOVERY_SERVER_PRESENT =
        ClassUtils.isPresent(GRAVITY_DISCOVERY_SERVER_CLASS, ClassPresentUtils.class.getClassLoader());

    private static final String CONSUL_SERVER_CLASS = "org.springframework.cloud.consul.discovery.ConsulServer";

    private static final boolean CONSUL_SERVER_CLASS_PRESENT =
        ClassUtils.isPresent(CONSUL_SERVER_CLASS, ClassPresentUtils.class.getClassLoader());

    public static boolean isGravityServerPresent() {
        return GRAVITY_DISCOVERY_SERVER_PRESENT;
    }

    public static boolean isConsulServerPresent() {
        return CONSUL_SERVER_CLASS_PRESENT;
    }

}
