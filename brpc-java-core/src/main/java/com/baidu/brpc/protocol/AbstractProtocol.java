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

package com.baidu.brpc.protocol;

import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public abstract class AbstractProtocol<P extends Packet> implements Protocol<P> {
    public ByteBuf encodeRequest(RpcRequest rpcRequest) throws Exception {
        return null;
    }

    public P decode(DynamicCompositeByteBuf in)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        return null;
    }

    public RpcResponse decodeResponse(P packet, ChannelHandlerContext ctx) throws Exception {
        return null;
    }


    public void decodeRequest(P packet, RpcRequest rpcRequest) throws Exception {
        return;
    }

    public ByteBuf encodeResponse(RpcResponse rpcResponse) throws Exception {
        return null;
    }

    public FullHttpRequest encodeHttpRequest(RpcRequest rpcRequest) throws Exception {
        return null;
    }

    public RpcResponse decodeHttpResponse(FullHttpResponse httpResponse,
                                          ChannelHandlerContext ctx) {
        return null;
    }

    public void decodeHttpRequest(FullHttpRequest httpRequest, RpcRequest rpcRequest) {
    }

    public FullHttpResponse encodeHttpResponse(RpcRequest rpcRequest, RpcResponse rpcResponse) {
        return null;
    }

    public boolean returnChannelBeforeResponse() {
        return true;
    }
}
