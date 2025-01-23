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

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;

/**
 * Invoker, invoke the rpc call and handle exceptions There are two types: Client and Server. Created by liuruisen on
 * 2019/12/18.
 */
public interface Invoker {
    /**
     * Invoke on the client or server side. Async call.
     * <p>
     * <b>Client side</b> Pack and serialize message. Delegate {@link ClientPeer} to implement real communication.
     * </p>
     *
     * <p>
     * <b>Server side</b> Unpack and deserialize message. Invoke the real method by java reflection.
     * </p>
     *
     * @param request request message
     * @param callback async callback
     */
    void invoke(Request request, RpcCallback callback);

    /**
     * Destroy itself and clear resources.
     *
     */
    default void destroy() {}

    /**
     * Init the invoker Init ClientPeer TODO ClientPeer init
     */
    default void init() {}

}
