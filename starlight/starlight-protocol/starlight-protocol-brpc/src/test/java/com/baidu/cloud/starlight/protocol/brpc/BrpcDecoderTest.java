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
 
package com.baidu.cloud.starlight.protocol.brpc;

import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class BrpcDecoderTest {

    private BrpcDecoder brpcDecoder = new BrpcDecoder();

    private BrpcEncoder brpcEncoder = new BrpcEncoder();

    private ByteBuf requestBuf;
    private ByteBuf responseBuf;

    private RpcRequest rpcRequest;

    private RpcResponse rpcResponse;

    @Before
    public void init() {
        Map<String, Object> kvMap = new HashMap<>();
        kvMap.put(BrpcProtocol.AUTH_KEY, "3456789");
        kvMap.put(Constants.TRACE_ID_KEY, 123l);
        kvMap.put(Constants.SPAN_ID_KEY, 2l);
        kvMap.put(Constants.PARENT_SPAN_ID_KEY, 1l);
        kvMap.put(BrpcProtocol.BINARY_ATTACH_KEY, "hello".getBytes());
        kvMap.put("Key1", "Value1");

        // starlight request meta test
        kvMap.put("Key3", 1L);
        kvMap.put("Key4", 123456);
        kvMap.put("Key5", 123.123f);

        brpcEncoder = new BrpcEncoder();

        rpcRequest = new RpcRequest();
        rpcRequest.setMethodName("init");
        rpcRequest.setServiceClass(this.getClass());
        rpcRequest.setServiceConfig(new ServiceConfig());
        rpcRequest.setParams(new Object[] {"Test"});
        rpcRequest.setParamsTypes(new Class[] {String.class});
        rpcRequest.setProtocolName("brpc");
        rpcRequest.setAttachmentKv(kvMap);

        brpcEncoder.encodeBody(rpcRequest);
        requestBuf = brpcEncoder.encode(rpcRequest);

        rpcResponse = new RpcResponse();
        rpcResponse.setResult("Test");
        rpcResponse.setReturnType(String.class);
        rpcResponse.setStatus(Constants.SUCCESS_CODE);
        rpcResponse.setErrorMsg("");
        rpcResponse.setProtocolName("brpc");
        rpcResponse.setRequest(rpcRequest);
        kvMap.remove(BrpcProtocol.BINARY_ATTACH_KEY);
        kvMap.remove(BrpcProtocol.BINARY_ATTACH_SIZE_KEY);
        rpcResponse.setAttachmentKv(kvMap);

        brpcEncoder.encodeBody(rpcResponse);
        responseBuf = brpcEncoder.encode(rpcResponse);
    }

    @Test
    public void decode() {
        DynamicCompositeByteBuf byteBuf = new DynamicCompositeByteBuf();
        byteBuf.addBuffer(requestBuf);
        RpcRequest rpcRequest = (RpcRequest) brpcDecoder.decode(byteBuf);
        // request rpc meta check
        Assert.assertTrue(rpcRequest.getMethodName().equals("init"));
        Assert.assertEquals(1L, rpcRequest.getAttachmentKv().get("Key3"));
        Assert.assertEquals(123456, rpcRequest.getAttachmentKv().get("Key4"));
        Assert.assertEquals(123.123f, rpcRequest.getAttachmentKv().get("Key5"));
        Assert.assertEquals("hello".getBytes().length,
            rpcRequest.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_SIZE_KEY));

        // request rpc body check
        rpcRequest.setParamsTypes(new Class[] {String.class});
        brpcDecoder.decodeBody(rpcRequest);
        Assert.assertTrue(rpcRequest.getParams()[0].equals("Test"));

        byteBuf.addBuffer(responseBuf);
        RpcResponse rpcResponse = (RpcResponse) brpcDecoder.decode(byteBuf);
        rpcResponse.setRequest(rpcRequest);
        Assert.assertTrue(rpcResponse.getStatus() == Constants.SUCCESS_CODE);
        rpcResponse.setReturnType(String.class);
        brpcDecoder.decodeBody(rpcResponse);
        Assert.assertTrue(rpcResponse.getResult().equals("Test"));
    }
}