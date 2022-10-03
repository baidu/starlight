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

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.transport.PeerStatus;

/**
 * StarlightClient: user interface used to Have both SingleStarlightClient and ClusterStarlightClient types Created by
 * liuruisen on 2020/1/3.
 */
public interface StarlightClient {

    /**
     * {@link URI} held by StarlightClient
     *
     * @return
     */
    URI remoteURI();

    /**
     * Init StarlightClient: init {@link ClientPeer} by Uri Uri: host:port + config params. in single model, host is ip;
     * in cluster model, host is service name
     */
    void init();

    /**
     * Generate {@link ClientInvoker} for the service Class
     * 
     * @param serviceClass
     * @param serviceConfig
     */
    void refer(Class<?> serviceClass, ServiceConfig serviceConfig);

    /**
     * Async Request: use {@link ClientInvoker} to call
     * 
     * @param request Request model
     * @param callback RpcCallback: BizWrapCallback or FutureCallback
     */
    void request(Request request, RpcCallback callback);

    /**
     * Clean resources And destroy self
     */
    void destroy();

    /**
     * If the starlight client is active Pay attention to the difference between cluster and single client scenarios
     * 
     * @return
     */
    boolean isActive();

    /**
     * Get the status of the SingleClient
     * 
     * @return
     */
    PeerStatus getStatus();

    /**
     * Can update status
     * 
     * @param newStatus
     */
    void updateStatus(PeerStatus newStatus);
}
