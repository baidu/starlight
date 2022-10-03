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

/**
 * Create a rpc server and expose the rpc services Please call {@link StarlightServer#serve()} to start server Created
 * by liuruisen on 2019/12/18.
 */
public interface StarlightServer {

    /**
     * init the StarlightServer
     */
    void init();

    /**
     * Start serve
     */
    void serve();

    /**
     * Destroy the server and Clear resources
     */
    void destroy();

    /**
     * export rpc service by InterfaceClass and rpcObject
     * 
     * @param rpcInterface
     * @param rpcObject
     */
    void export(Class<?> rpcInterface, Object rpcObject);

    /**
     * export rpc service by InterfaceClass and rpcObject
     * 
     * @param rpcInterface
     * @param rpcObject
     * @param serviceConfig
     */
    void export(Class<?> rpcInterface, Object rpcObject, ServiceConfig serviceConfig);

    /**
     * Export By {@link RpcService}
     * 
     * @param rpcService
     */
    void export(RpcService rpcService);

    /**
     * unExport the service
     * 
     * @param rpcInterface
     */
    void unexport(Class<?> rpcInterface);

    /**
     * Unexport by {@link RpcService}
     * 
     * @param rpcService
     */
    void unexport(RpcService rpcService);

}
