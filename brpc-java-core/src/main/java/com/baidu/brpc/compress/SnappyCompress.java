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

import com.google.protobuf.Message;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.buffer.DynamicCompositeByteBufInputStream;
import com.baidu.brpc.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class SnappyCompress implements Compress {
    @Override
    public ByteBuf compressInput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException {
        byte[] bytes = rpcMethodInfo.inputEncode(proto);
        int maxCompressedSize = Snappy.maxCompressedLength(bytes.length);
        byte[] compressedBytes = new byte[maxCompressedSize];
        int compressedLen = Snappy.compress(bytes, 0, bytes.length, compressedBytes, 0);
        return Unpooled.wrappedBuffer(compressedBytes, 0, compressedLen);
    }

    @Override
    public Object uncompressOutput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteBufInputStream(byteBuf);
        inputStream = new SnappyInputStream(inputStream);
        if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
            ProtobufRpcMethodInfo protobufRpcMethodInfo = (ProtobufRpcMethodInfo) rpcMethodInfo;
            Message proto = (Message) protobufRpcMethodInfo.outputDecode(inputStream);
            return proto;
        } else {
            byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
            return rpcMethodInfo.outputDecode(uncompressedBytes);
        }
    }

    @Override
    public Object uncompressOutput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        inputStream = new SnappyInputStream(inputStream);
        if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
            ProtobufRpcMethodInfo protobufRpcMethodInfo = (ProtobufRpcMethodInfo) rpcMethodInfo;
            Message proto = (Message) protobufRpcMethodInfo.outputDecode(inputStream);
            return proto;
        } else {
            byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
            return rpcMethodInfo.outputDecode(uncompressedBytes);
        }
    }

    @Override
    public Object uncompressOutput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new DynamicCompositeByteBufInputStream(byteBuf);
        inputStream = new SnappyInputStream(inputStream);
        if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
            ProtobufRpcMethodInfo protobufRpcMethodInfo = (ProtobufRpcMethodInfo) rpcMethodInfo;
            Message proto = (Message) protobufRpcMethodInfo.outputDecode(inputStream);
            return proto;
        } else {
            byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
            return rpcMethodInfo.outputDecode(uncompressedBytes);
        }
    }

    @Override
    public Object uncompressInput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteBufInputStream(byteBuf);
        inputStream = new SnappyInputStream(inputStream);
        if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
            return ((ProtobufRpcMethodInfo) rpcMethodInfo).inputDecode(inputStream);
        } else {
            byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
            return rpcMethodInfo.inputDecode(uncompressedBytes);
        }
    }

    @Override
    public Object uncompressInput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        inputStream = new SnappyInputStream(inputStream);
        if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
            return ((ProtobufRpcMethodInfo) rpcMethodInfo).inputDecode(inputStream);
        } else {
            byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
            return rpcMethodInfo.inputDecode(uncompressedBytes);
        }
    }

    @Override
    public Object uncompressInput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new DynamicCompositeByteBufInputStream(byteBuf);
        inputStream = new SnappyInputStream(inputStream);
        if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
            return ((ProtobufRpcMethodInfo) rpcMethodInfo).inputDecode(inputStream);
        } else {
            byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
            return rpcMethodInfo.inputDecode(uncompressedBytes);
        }
    }

    @Override
    public ByteBuf compressOutput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException {
        byte[] bytes = rpcMethodInfo.outputEncode(proto);
        int maxCompressedSize = Snappy.maxCompressedLength(bytes.length);
        byte[] compressedBytes = new byte[maxCompressedSize];
        int compressedLen = Snappy.compress(bytes, 0, bytes.length, compressedBytes, 0);
        return Unpooled.wrappedBuffer(compressedBytes, 0, compressedLen);
    }

}
