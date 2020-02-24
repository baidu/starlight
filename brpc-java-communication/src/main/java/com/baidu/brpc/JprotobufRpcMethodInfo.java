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

package com.baidu.brpc;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.google.protobuf.CodedOutputStream;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * jprotobuf method info, which can be encode/decode jprotobuf class.
 * details for jprotobuf: https://github.com/jhunters/jprotobuf
 */
@Setter
@Getter
public class JprotobufRpcMethodInfo extends RpcMethodInfo {
    private Codec inputCodec;
    private Codec outputCodec;

    public JprotobufRpcMethodInfo(Method method) {
        super(method);
        inputCodec = ProtobufProxy.create((Class) (inputClasses[0]));
        outputCodec = ProtobufProxy.create((Class) outputClass);
    }

    @Override
    public byte[] inputEncode(Object input) throws IOException {
        if (inputCodec != null) {
            return inputCodec.encode(input);
        }
        return null;
    }

    @Override
    public void inputWriteToStream(Object input, CodedOutputStream stream) throws IOException {
        if (inputCodec != null) {
            inputCodec.writeTo(input, stream);
        }
    }

    @Override
    public Object outputDecode(byte[] output) throws IOException {
        if (outputCodec != null) {
            return outputCodec.decode(output);
        }
        return null;
    }

    @Override
    public Object outputDecode(ByteBuf output) throws IOException {
        if (outputCodec != null) {
            int len = output.readableBytes();
            byte[] bytes = new byte[len];
            output.readBytes(bytes);
            return outputCodec.decode(bytes);
        }
        return null;
    }

    @Override
    public Object outputDecode(DynamicCompositeByteBuf output) throws IOException {
        if (outputCodec != null) {
            int len = output.readableBytes();
            byte[] bytes = new byte[len];
            output.readBytes(bytes);
            return outputCodec.decode(bytes);
        }
        return null;
    }

    @Override
    public Object inputDecode(byte[] input) throws IOException {
        if (inputCodec != null) {
            return inputCodec.decode(input);
        }
        return null;
    }

    @Override
    public Object inputDecode(ByteBuf input) throws IOException {
        if (inputCodec != null) {
            int len = input.readableBytes();
            byte[] bytes = new byte[len];
            input.readBytes(bytes);
            return inputCodec.decode(bytes);
        }
        return null;
    }

    @Override
    public Object inputDecode(DynamicCompositeByteBuf input) throws IOException {
        if (inputCodec != null) {
            int len = input.readableBytes();
            byte[] bytes = new byte[len];
            input.readBytes(bytes);
            return inputCodec.decode(bytes);
        }
        return null;
    }

    @Override
    public byte[] outputEncode(Object output) throws IOException {
        if (outputCodec != null) {
            return outputCodec.encode(output);
        }
        return null;
    }

    @Override
    public void outputWriteToStream(Object output, CodedOutputStream stream) throws IOException {
        if (outputCodec != null) {
            outputCodec.writeTo(output, stream);
        }
    }

    @Override
    public int getInputSerializedSize(Object input) throws IOException {
        if (inputCodec != null) {
            return inputCodec.size(input);
        }
        return 0;
    }

    @Override
    public int getOutputSerializedSize(Object output) throws IOException {
        if (outputCodec != null) {
            return outputCodec.size(output);
        }
        return 0;
    }
}
