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

import io.netty.buffer.ByteBuf;
import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.AbstractMsgBase;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.baidu.cloud.starlight.api.exception.CodecException.BODY_ENCODE_EXCEPTION;
import static com.baidu.cloud.starlight.api.exception.CodecException.PROTOCOL_ENCODE_EXCEPTION;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class BrpcEncoderTest {

    private RpcRequest rpcRequest;

    private RpcResponse rpcResponse;

    private BrpcEncoder brpcEncoder;

    @Before
    public void init() {
        Map<String, Object> kvMap = new HashMap<>();
        kvMap.put(BrpcProtocol.AUTH_KEY, "3456789");
        kvMap.put(Constants.TRACE_ID_KEY, 123l);
        kvMap.put(Constants.SPAN_ID_KEY, 2l);
        kvMap.put(Constants.PARENT_SPAN_ID_KEY, 1l);
        kvMap.put(BrpcProtocol.BINARY_ATTACH_KEY, "hello".getBytes());
        kvMap.put("Key1", "Value1");

        rpcRequest = new RpcRequest();
        rpcRequest.setMethodName("init");
        rpcRequest.setServiceClass(this.getClass());
        rpcRequest.setServiceConfig(new ServiceConfig());
        rpcRequest.setParams(new Object[] {"Test"});
        rpcRequest.setParamsTypes(new Class[] {String.class});
        rpcRequest.setProtocolName("brpc");
        rpcRequest.setAttachmentKv(kvMap);

        rpcResponse = new RpcResponse();
        rpcResponse.setRequest(rpcRequest);
        rpcResponse.setResult("Test");
        rpcResponse.setReturnType(String.class);
        rpcResponse.setStatus(Constants.SUCCESS_CODE);
        rpcResponse.setErrorMsg("");
        rpcResponse.setProtocolName("brpc");
        rpcResponse.setAttachmentKv(kvMap);

        brpcEncoder = new BrpcEncoder();
    }

    @Test
    public void encode() {
        // encode request
        ByteBuf requestByte = brpcEncoder.encode(rpcRequest);
        Assert.assertTrue(requestByte.readableBytes() > 0);
        System.out.println(requestByte.readInt());
        System.out.println(requestByte.readByte());

        // encode response
        ByteBuf responseByte = brpcEncoder.encode(rpcResponse);
        Assert.assertTrue(responseByte.readableBytes() > 0);
        System.out.println(responseByte.readInt());
        System.out.println(responseByte.readByte());

        // encode null
        try {
            brpcEncoder.encode(null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == PROTOCOL_ENCODE_EXCEPTION);
        }

        // encode error type message
        try {
            brpcEncoder.encode(new AbstractMsgBase() {});
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == PROTOCOL_ENCODE_EXCEPTION);
        }
    }

    @Test
    public void encodeBody() {
        // encode request body
        brpcEncoder.encodeBody(rpcRequest);
        Assert.assertTrue(rpcRequest.getBodyBytes().length > 0);
        Integer attachBinartSize = (Integer) rpcRequest.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_SIZE_KEY);
        Assert.assertEquals("hello".getBytes().length, (int) attachBinartSize);

        // encode response body
        brpcEncoder.encodeBody(rpcResponse);
        Assert.assertTrue(rpcResponse.getBodyBytes().length > 0);

        // encode null
        try {
            brpcEncoder.encodeBody(null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == BODY_ENCODE_EXCEPTION);
        }

        // encode error type message
        MsgBase msgBase = new AbstractMsgBase() {};
        msgBase.setBodyBytes(new byte[0]);
        brpcEncoder.encodeBody(msgBase);
        Assert.assertTrue(msgBase.getBodyBytes().length == 0);
    }
}