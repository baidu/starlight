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
 
package com.baidu.cloud.starlight.protocol.stargate;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/7/28.
 */
public class StargateDecoderTest {

    private StargateDecoder decoder = new StargateDecoder();

    private StargateEncoder encoder = new StargateEncoder();

    @Test
    public void decodeRequest() {
        Request request = new RpcRequest();
        request.setProtocolName(StargateProtocol.PROTOCOL_NAME);
        request.setMethodName("method");
        request.setParams(new Object[] {"1313"});
        request.setParamsTypes(new Class[] {String.class});
        request.setServiceClass(StargateEncoder.class);
        request.setServiceConfig(new ServiceConfig());

        encoder.encodeBody(request);

        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        compositeByteBuf.addBuffer(encoder.encode(request));
        Assert.assertTrue(compositeByteBuf.readableBytes() > 0);

        MsgBase msgBase = decoder.decode(compositeByteBuf);
        Assert.assertTrue(msgBase instanceof Request);
        assertEquals(0, compositeByteBuf.readableBytes());
        Request request1 = (Request) msgBase;
        assertEquals(StargateEncoder.class.getName(), request1.getServiceName());
        assertEquals("method", request1.getMethodName());
    }

    @Test
    public void decodeResponse() {
        Response response = new RpcResponse();
        response.setStatus(200);
        response.setResult("Test");
        response.setRequest(new RpcRequest());

        encoder.encodeBody(response);

        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        compositeByteBuf.addBuffer(encoder.encode(response));
        Assert.assertTrue(compositeByteBuf.readableBytes() > 0);

        MsgBase msgBase = decoder.decode(compositeByteBuf);
        Assert.assertTrue(msgBase instanceof Response);
        assertEquals(0, compositeByteBuf.readableBytes());

        Response response1 = (Response) msgBase;
        assertEquals(Constants.SUCCESS_CODE.intValue(), response1.getStatus());
        assertEquals("Test", response1.getResult());

        Response errResponse = new RpcResponse();
        errResponse.setStatus(StarlightRpcException.BIZ_ERROR);
        errResponse.setErrorMsg("Error");
        errResponse.setRequest(new RpcRequest());

        encoder.encodeBody(errResponse);
        compositeByteBuf.addBuffer(encoder.encode(errResponse));
        Assert.assertTrue(compositeByteBuf.readableBytes() > 0);
        MsgBase msgBase2 = decoder.decode(compositeByteBuf);
        Assert.assertTrue(msgBase2 instanceof Response);
        assertEquals(0, compositeByteBuf.readableBytes());
        Response response2 = (Response) msgBase2;
        assertEquals(StarlightRpcException.BIZ_ERROR.intValue(), response2.getStatus());
        assertTrue(response2.getErrorMsg().contains("Server had occur exception"));
    }

    @Test
    public void decodeBody() {
        decoder.decodeBody(null);
    }
}