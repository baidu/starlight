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

import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * RpcChannel. Used to Connect Channel, Write Message, Read Message Created by liuruisen on 2019/11/27.
 */
public interface RpcChannel {

    /**
     * Channel attribute
     **/
    AttributeKey<ChannelAttribute> ATTRIBUTE_KEY = AttributeKey.valueOf("channel.attribute");

    /**
     * RpcChannel Side
     * 
     * @return
     */
    ChannelSide side();

    /**
     * Return the remote address when the channel is connected to.
     * 
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Return the local address when the channel is bound to. fixme 是否需要
     * 
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * RpcChannel is active
     * 
     * @return
     */
    boolean isActive();

    /**
     * Get real network communication Netty {@link Channel} The channel must active ?
     *
     * @return
     */
    Channel channel();

    /**
     * reconnect the channel
     * 
     * @return
     */
    void reconnect() throws TransportException;

    /**
     * Init RpcChannel
     */
    void init();

    /**
     * Close Channel and clear resources Gracefully shutdown NOTICE: This stage may be time-consuming
     */
    void close();

    /**
     * Send message. Select a real Netty Channel to writeAndFlush message Or change to getChannel()
     *
     * @param msgBase
     * @throws TransportException
     */
    void send(MsgBase msgBase) throws TransportException;

    /**
     * Receive message. Do something when receive message, such as: statistic
     *
     * @param msgBase
     */
    void receive(MsgBase msgBase);

    /**
     * Register Callback to Channel, this callback is handle through the channel
     * 
     * @param id
     * @param callback
     */
    void putCallback(long id, RpcCallback callback);

    /**
     * Remove RpcCallback by id
     * 
     * @param id
     * @return
     */
    RpcCallback removeCallback(long id);

    /**
     * Get all callbacks processed by this instance
     * 
     * @return
     */
    Map<Long, RpcCallback> allCallbacks();

    /**
     * Allow to store a value reference. It is thread-safe
     * 
     * @param attributeKey
     * @param attributeVal
     */
    void setAttribute(String attributeKey, Object attributeVal);

    /**
     * Return the attribute releated to the key
     * 
     * @param attributeKey
     * @return
     */
    Object getAttribute(String attributeKey);

    /**
     * Get RpcChannel linked RpcChannelGroup
     * 
     * @return
     */
    RpcChannelGroup getRpcChannelGroup();

}