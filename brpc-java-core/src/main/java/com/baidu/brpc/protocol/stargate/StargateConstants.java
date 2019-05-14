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

package com.baidu.brpc.protocol.stargate;

import java.util.regex.Pattern;

/**
 * copy from Stargate 1.2.18
 */
public interface StargateConstants {

    /**
     * version
     */
    String STARGATE_VERSION = "1.2.18";

    /**
     * 全局采用逗号分隔
     */
    Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    /**
     * URI里指定默认值,比如有key,那么DEFAULT_KEY_PREFIX+key指定的值就是该key的默认值
     */
    String DEFAULT_KEY_PREFIX = "default.";

    /**
     * 服务分组key
     */
    String GROUP_KEY = "group";

    /**
     * 服务接口key
     */
    String INTERFACE_KEY = "interface";

    /**
     * 服务版本key
     */
    String VERSION_KEY = "version";

    /**
     * 服务简单key
     */
    String INTERFACE_SIMPLE_KEY = "interface.simple";

    /**
     * zk 上 consumer 目录
     */
    String ZK_CONSUMER_DIR = "consumer";

}
