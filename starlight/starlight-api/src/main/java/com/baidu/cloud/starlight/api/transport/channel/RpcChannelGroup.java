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
 
package com.baidu.cloud.starlight.api.transport.channel;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;

import java.util.Set;

/**
 *
 * Created by liuruisen on 2020/11/25.
 */
public interface RpcChannelGroup {

    /**
     * Init and prepare RpcChannels
     */
    void init();

    /**
     * Get Channel Uri
     *
     * @return
     */
    URI getUri();

    /**
     * Randomly get or create one RpcChannel from the group The RpcChannel returned must be active.
     *
     * NOTICE: This method may connect to remote and may throw transport exception
     * 
     * @return
     */
    RpcChannel getRpcChannel() throws TransportException;

    /**
     * Return the RpcChannel to group, so it can be reuse
     * 
     * @param rpcChannel
     */
    void returnRpcChannel(RpcChannel rpcChannel);

    /**
     * Remove the RpcChannel from group. Will create a new RpcChannel and add to the group
     * 
     * @param rpcChannel
     */
    void removeRpcChannel(RpcChannel rpcChannel);

    /**
     * How many RpcChannel in the group
     * 
     * @return
     */
    int rpcChannelCount();

    /**
     * Close all RpcChannel in the RpcChannelGroup
     */
    void close();

    /**
     * Get All RpcChannels in the group
     * 
     * @return
     */
    Set<RpcChannel> allRpcChannels();
}
