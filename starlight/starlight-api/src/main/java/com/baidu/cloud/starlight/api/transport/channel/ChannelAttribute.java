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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;

/**
 * Created by liuruisen on 2020/3/31.
 */
public class ChannelAttribute {

    // Channel dimension ByteBuf
    private DynamicCompositeByteBuf dynamicByteBuf;

    private final RpcChannel rpcChannel;

    public ChannelAttribute(RpcChannel rpcChannel) {
        this.rpcChannel = rpcChannel;
        this.dynamicByteBuf = new DynamicCompositeByteBuf(16);
    }

    public DynamicCompositeByteBuf getDynamicByteBuf() {
        return dynamicByteBuf;
    }

    public RpcChannel getRpcChannel() {
        return rpcChannel;
    }

    public String getChannelProtocol() {
        return (String) rpcChannel.getAttribute(Constants.PROTOCOL_ATTR_KEY);
    }

    public void resetChannelProtocol(String protocolName) {
        rpcChannel.setAttribute(Constants.PROTOCOL_ATTR_KEY, protocolName);
    }
}
