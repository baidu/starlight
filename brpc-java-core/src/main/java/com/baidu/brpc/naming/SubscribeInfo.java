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
package com.baidu.brpc.naming;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO class of subscribe info.
 *
 * @author xiemalin
 */
@Setter
@Getter
@EqualsAndHashCode
public class SubscribeInfo extends NamingOptions {
    /**
     * the interface class name.
     */
    private String interfaceName;

    public SubscribeInfo() {
    }

    public SubscribeInfo(SubscribeInfo rhs) {
        super(rhs);
        this.interfaceName = rhs.getInterfaceName();
    }

    public SubscribeInfo(NamingOptions namingOptions) {
        super(namingOptions);
    }

}
