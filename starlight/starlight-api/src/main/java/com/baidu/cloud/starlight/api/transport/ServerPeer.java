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
 
package com.baidu.cloud.starlight.api.transport;

import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;

import java.util.Map;

/**
 * ServerPeer: Netty Server Bind to ip + port, accept connection and request. Use NettyInboundHandler to handle request.
 * Created by liuruisen on 2019/11/27.
 */
public interface ServerPeer extends Peer {

    /**
     * Bind to settled Url(IP + Port)
     */
    void bind();

    /**
     * Is bound
     * 
     * @return
     */
    boolean isBound();

    /**
     * RpcChannels connected to the server The method must be thread-safe
     * 
     * @return
     */
    Map<String, RpcChannel> rpcChannels();

}
