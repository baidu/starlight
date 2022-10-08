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
 
package com.baidu.cloud.starlight.api.rpc;

import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.ClientPeer;

/**
 * Client side Invoker use {@link ClientPeer} to communicate with server. Created by liuruisen on 2019/12/18.
 */
public interface ClientInvoker extends Invoker {

    /**
     * Network communication using ClientPeer: default is NettyClient
     * 
     * @return ClientPeer
     */
    ClientPeer getClientPeer();

    /**
     * Target interface information represented by ClientInvoker
     * 
     * @return ServiceConfig
     */
    ServiceConfig getServiceConfig();
}
