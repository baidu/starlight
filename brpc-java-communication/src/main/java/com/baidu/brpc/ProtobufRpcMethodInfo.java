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

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.utils.ProtobufUtils;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

@Setter
@Getter
public class ProtobufRpcMethodInfo extends RpcMethodInfo {
    private Message inputInstance;
    private Method inputParseFromMethod;
    private Method inputGetDefaultInstanceMethod;

    private Message outputInstance;
    private Method outputParseFromMethod;
    private Method outputGetDefaultInstanceMethod;

    public ProtobufRpcMethodInfo(Method method) {
        super(method);
        try {
            this.inputGetDefaultInstanceMethod = ((Class) inputClasses[0]).getMethod("getDefaultInstance");
            this.inputInstance = (Message) inputGetDefaultInstanceMethod.invoke(null);
            this.inputParseFromMethod = ((Class) inputClasses[0]).getMethod("parseFrom", byte[].class);

            this.outputGetDefaultInstanceMethod = ((Class) outputClass).getMethod("getDefaultInstance");
            this.outputInstance = (Message) outputGetDefaultInstanceMethod.invoke(null);
            this.outputParseFromMethod = ((Class) outputClass).getMethod("parseFrom", byte[].class);

        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public byte[] inputEncode(Object input) throws IOException {
        if (input instanceof Message) {
            return ((Message) input).toByteArray();
        }
        return null;
    }

    @Override
    public void inputWriteToStream(Object input, CodedOutputStream stream) throws IOException {
        if (input instanceof Message) {
            ((Message) input).writeTo(stream);
            stream.flush();
        }
    }

    @Override
    public Object outputDecode(byte[] output) throws IOException {
        if (outputParseFromMethod != null && output != null) {
            try {
                return outputParseFromMethod.invoke(outputClass, output);
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public Object outputDecode(ByteBuf output) throws IOException {
        return ProtobufUtils.parseFrom(output, outputInstance);
    }

    @Override
    public Object outputDecode(DynamicCompositeByteBuf output) throws IOException {
        return ProtobufUtils.parseFrom(output, outputInstance);
    }

    public Object outputDecode(InputStream stream) throws IOException {
        if (outputGetDefaultInstanceMethod != null && stream != null) {
            try {
                Message proto = (Message) outputGetDefaultInstanceMethod.invoke(null);
                proto = proto.newBuilderForType().mergeFrom(stream).build();
                return proto;
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return null;
    }

    public Object inputDecode(byte[] input) throws IOException {
        return inputInstance.getParserForType().parseFrom(input);
    }

    public Object inputDecode(byte[] input, int offset, int len) throws IOException {
        return inputInstance.getParserForType().parseFrom(input, offset, len);
    }

    public Object inputDecode(ByteBuf input) throws IOException {
        return ProtobufUtils.parseFrom(input, inputInstance);
    }

    public Object inputDecode(DynamicCompositeByteBuf input) throws IOException {
        return ProtobufUtils.parseFrom(input, inputInstance);
    }

    public Object inputDecode(InputStream stream) throws IOException {
        if (inputGetDefaultInstanceMethod != null && stream != null) {
            try {
                Message proto = (Message) inputGetDefaultInstanceMethod.invoke(null);
                proto = proto.newBuilderForType().mergeFrom(stream).build();
                return proto;
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return null;
    }

    public byte[] outputEncode(Object output) throws IOException {
        if (output instanceof Message) {
            return ((Message) output).toByteArray();
        }
        return null;
    }

    @Override
    public void outputWriteToStream(Object output, CodedOutputStream stream) throws IOException {
        if (output instanceof Message) {
            ((Message) output).writeTo(stream);
            stream.flush();
        }
    }

    @Override
    public int getInputSerializedSize(Object input) throws IOException {
        if (input instanceof Message) {
            return ((Message) input).getSerializedSize();
        }
        return 0;
    }

    @Override
    public int getOutputSerializedSize(Object output) throws IOException {
        if (output instanceof Message) {
            return ((Message) output).getSerializedSize();
        }
        return 0;
    }
}
