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

import java.io.IOException;

public interface Compress {
    /**
     * client端encode request并压缩
     * @param proto request
     * @param rpcMethodInfo rpc method信息
     * @return 序列化后buf
     * @throws IOException 序列化失败
     */
    ByteBuf compressInput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException;

    /**
     * client端解压缩并反序列化response
     * @param byteBuf response buffer
     * @param rpcMethodInfo rpc method信息
     * @return response对象
     * @throws IOException 反序列化失败
     */
    Object uncompressOutput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException;

    Object uncompressOutput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException;

    Object uncompressOutput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException;

    /**
     * server序列化response并压缩
     * @param proto response对象
     * @param rpcMethodInfo rpc method信息
     * @return 序列化buffer
     * @throws IOException 序列化异常
     */
    ByteBuf compressOutput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException;

    /**
     * server端解压缩并decode request
     * @param byteBuf request buffer
     * @param rpcMethodInfo rpc method信息
     * @return 请求对象
     * @throws IOException 反序列化失败
     */
    Object uncompressInput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException;

    Object uncompressInput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException;

    Object uncompressInput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException;
}
