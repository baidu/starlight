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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.protocol.nshead.NSHeadMeta;
import com.baidu.brpc.utils.RpcMetaUtils;
import com.baidu.brpc.utils.ThreadPool;
import com.google.protobuf.CodedOutputStream;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

/**
 * rpc method info is parsed when application initialized.
 * it can be both used at client and server side.
 */
@Setter
@Getter
public class RpcMethodInfo {
    protected Method method;
    protected String serviceName;
    protected String methodName;
    protected Type[] inputClasses;
    protected Type outputClass;
    protected NSHeadMeta nsHeadMeta;
    // instance of interface which method belongs to
    protected Object target;
    protected ThreadPool threadPool;

    public RpcMethodInfo(Method method) {
        RpcMetaUtils.RpcMetaInfo metaInfo = RpcMetaUtils.parseRpcMeta(method);
        this.serviceName = metaInfo.getServiceName();
        this.methodName = metaInfo.getMethodName();
        this.method = method;
        Type[] inputClasses = method.getGenericParameterTypes();
        if (inputClasses.length < 0) {
            throw new IllegalArgumentException("invalid params");
        }
        this.inputClasses = inputClasses;
        this.outputClass = method.getGenericReturnType();
        this.nsHeadMeta = method.getAnnotation(NSHeadMeta.class);
    }

    /**
     * encode request at client inside
     *
     * @param input the input
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public byte[] inputEncode(Object input) throws IOException {
        return null;
    }

    /**
     * encode request at client inside
     * @param input request
     * @param stream out buffer stream
     * @throws IOException io exception
     */
    public void inputWriteToStream(Object input, CodedOutputStream stream) throws IOException {
    }

    /**
     * decode response at client side
     *
     * @param output response byte array
     * @return response proto object
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Object outputDecode(byte[] output) throws IOException {
        return null;
    }

    /**
     * decode response at client side
     * @param output response netty ByteBuf
     * @return response proto object
     * @throws IOException
     */
    public Object outputDecode(ByteBuf output) throws IOException {
        return null;
    }

    public Object outputDecode(DynamicCompositeByteBuf output) throws IOException {
        return null;
    }

    /**
     * decode request at server side
     * @param input request byte array
     * @return request proto instance
     * @throws IOException
     */
    public Object inputDecode(byte[] input) throws IOException {
        return null;
    }

    /**
     * decode request at server side
     * @param input request netty {@link ByteBuf}
     * @return request proto instance
     * @throws IOException
     */
    public Object inputDecode(ByteBuf input) throws IOException {
        return null;
    }

    public Object inputDecode(DynamicCompositeByteBuf input) throws IOException {
        return null;
    }

    /**
     * encode response proto instance at server side
     * @param output response proto instance
     * @return encoded byte array
     * @throws IOException
     */
    public byte[] outputEncode(Object output) throws IOException {
        return null;
    }

    /**
     * encode response to stream at server side
     * @param output response object
     * @param stream output stream
     * @throws IOException
     */
    public void outputWriteToStream(Object output, CodedOutputStream stream) throws IOException {
    }

    /**
     * get serialized size of request proto instance
     * @param input proto instance
     * @return serialized size
     * @throws IOException
     */
    public int getInputSerializedSize(Object input) throws IOException {
        return 0;
    }

    /**
     * get serialized size of response proto instance
     * @param output response object
     * @return serialized size
     * @throws IOException
     */
    public int getOutputSerializedSize(Object output) throws IOException {
        return 0;
    }
}
