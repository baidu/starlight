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

package com.baidu.brpc.compress;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class NoneCompress implements Compress {
    @Override
    public ByteBuf compressInput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException {
        byte[] bytes = rpcMethodInfo.inputEncode(proto);
        return Unpooled.wrappedBuffer(bytes);
    }

    @Override
    public Object uncompressOutput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        return rpcMethodInfo.outputDecode(byteBuf);
    }

    @Override
    public Object uncompressOutput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException {
        return rpcMethodInfo.outputDecode(bytes);
    }

    @Override
    public Object uncompressOutput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        return rpcMethodInfo.outputDecode(byteBuf);
    }

    @Override
    public ByteBuf compressOutput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException {
        byte[] bytes = rpcMethodInfo.outputEncode(proto);
        return Unpooled.wrappedBuffer(bytes);
    }

    @Override
    public Object uncompressInput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        return rpcMethodInfo.inputDecode(byteBuf);
    }

    @Override
    public Object uncompressInput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException {
        return rpcMethodInfo.inputDecode(bytes);
    }

    @Override
    public Object uncompressInput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        return rpcMethodInfo.inputDecode(byteBuf);
    }
}
