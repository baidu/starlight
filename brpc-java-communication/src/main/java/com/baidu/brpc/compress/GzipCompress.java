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

import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.buffer.DynamicCompositeByteBufInputStream;
import com.baidu.brpc.utils.IOUtils;
import com.google.protobuf.CodedOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class GzipCompress implements Compress {
    @Override
    public ByteBuf compressInput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException {
        int protoSize = rpcMethodInfo.getInputSerializedSize(proto);
        ByteBuf resBuf = Unpooled.buffer(protoSize);
        OutputStream outputStream = new ByteBufOutputStream(resBuf);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

        if (protoSize > CodedOutputStream.DEFAULT_BUFFER_SIZE) {
            protoSize = CodedOutputStream.DEFAULT_BUFFER_SIZE;
        }
        final CodedOutputStream codedOutputStream =
                CodedOutputStream.newInstance(gzipOutputStream, protoSize);
        rpcMethodInfo.inputWriteToStream(proto, codedOutputStream);
        gzipOutputStream.close();
        return resBuf;
    }

    @Override
    public Object uncompressOutput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteBufInputStream(byteBuf);
        inputStream = new GZIPInputStream(inputStream);
        try {
            if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                return ((ProtobufRpcMethodInfo) rpcMethodInfo).outputDecode(inputStream);
            } else {
                byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
                return rpcMethodInfo.outputDecode(uncompressedBytes);
            }
        } finally {
            inputStream.close();
        }
    }

    @Override
    public Object uncompressOutput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        inputStream = new GZIPInputStream(inputStream);
        try {
            if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                return ((ProtobufRpcMethodInfo) rpcMethodInfo).outputDecode(inputStream);
            } else {
                byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
                return rpcMethodInfo.outputDecode(uncompressedBytes);
            }
        } finally {
            inputStream.close();
        }
    }

    @Override
    public Object uncompressOutput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new DynamicCompositeByteBufInputStream(byteBuf);
        inputStream = new GZIPInputStream(inputStream);
        try {
            if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                return ((ProtobufRpcMethodInfo) rpcMethodInfo).outputDecode(inputStream);
            } else {
                byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
                return rpcMethodInfo.outputDecode(uncompressedBytes);
            }
        } finally {
            inputStream.close();
        }
    }

    @Override
    public Object uncompressInput(ByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteBufInputStream(byteBuf);
        inputStream = new GZIPInputStream(inputStream);
        try {
            if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                return ((ProtobufRpcMethodInfo) rpcMethodInfo).inputDecode(inputStream);
            } else {
                byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
                return rpcMethodInfo.inputDecode(uncompressedBytes);
            }
        } finally {
            inputStream.close();
        }
    }

    @Override
    public Object uncompressInput(byte[] bytes, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        inputStream = new GZIPInputStream(inputStream);
        try {
            if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                return ((ProtobufRpcMethodInfo) rpcMethodInfo).inputDecode(inputStream);
            } else {
                byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
                return rpcMethodInfo.inputDecode(uncompressedBytes);
            }
        } finally {
            inputStream.close();
        }
    }

    public Object uncompressInput(DynamicCompositeByteBuf byteBuf, RpcMethodInfo rpcMethodInfo) throws IOException {
        InputStream inputStream = new DynamicCompositeByteBufInputStream(byteBuf);
        inputStream = new GZIPInputStream(inputStream);
        try {
            if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                return ((ProtobufRpcMethodInfo) rpcMethodInfo).inputDecode(inputStream);
            } else {
                byte[] uncompressedBytes = IOUtils.readInputStream(inputStream);
                return rpcMethodInfo.inputDecode(uncompressedBytes);
            }
        } finally {
            inputStream.close();
        }
    }

    @Override
    public ByteBuf compressOutput(Object proto, RpcMethodInfo rpcMethodInfo) throws IOException {
        int protoSize = rpcMethodInfo.getOutputSerializedSize(proto);
        ByteBuf resBuf = Unpooled.buffer(protoSize);
        OutputStream outputStream = new ByteBufOutputStream(resBuf);
        outputStream = new GZIPOutputStream(outputStream);

        if (protoSize > CodedOutputStream.DEFAULT_BUFFER_SIZE) {
            protoSize = CodedOutputStream.DEFAULT_BUFFER_SIZE;
        }
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream, protoSize);
        rpcMethodInfo.outputWriteToStream(proto, codedOutputStream);
        outputStream.close();
        return resBuf;
    }
}
