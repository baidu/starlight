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
package com.baidu.brpc.naming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO class of register info.
 * 
 * @author xiemalin
 * @since 2.27
 */
@Setter
@Getter
@AllArgsConstructor
public class RegisterInfo {

    /** host info. */
    private String host;

    /** The port. */
    private int port;

    /** the unique mark for each service info. */
    private String service;

    private String group;

    private String version;

    public RegisterInfo(String service, String group, String version) {
        this.service = service;
        this.group = group;
        this.version = version;
    }
}
