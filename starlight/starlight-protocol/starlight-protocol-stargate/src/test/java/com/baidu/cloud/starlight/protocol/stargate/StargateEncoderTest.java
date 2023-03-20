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

import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import io.netty.buffer.ByteBuf;
import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.serialization.serializer.DyuProtostuffSerializer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/7/27.
 */
public class StargateEncoderTest {

    private StargateEncoder encoder = new StargateEncoder();

    private DyuProtostuffSerializer serializer = new DyuProtostuffSerializer();

    @Test
    public void encodeRequest() {
        Request request = encodeRequestBody();
        ByteBuf byteBuf = encoder.encode(request);
        byte[] fixBytes = new byte[5];
        byteBuf.readBytes(fixBytes);
        Assert.assertEquals(fixBytes[4], 10);
    }

    @Test
    public void encodeError() {
        // encode body null
        Request request = null;
        try {
            encoder.encodeBody(request);
        } catch (CodecException e) {
            Assert.assertEquals(e.getCode(), CodecException.PROTOCOL_ENCODE_EXCEPTION);
        }

        // stargate encode without group or version
        request = new RpcRequest();
        try {
            encoder.encodeBody(request);
        } catch (CodecException e) {
            Assert.assertEquals(e.getCode(), CodecException.PROTOCOL_ENCODE_EXCEPTION);
        }

        // encode null
        try {
            encoder.encode(null);
        } catch (CodecException e) {
            Assert.assertEquals(e.getCode(), CodecException.PROTOCOL_ENCODE_EXCEPTION);
        }
    }

    @Test
    public void encodeResponse() {
        Response response = encodeResponseBody();
        ByteBuf byteBuf = encoder.encode(response);
        byte[] fixBytes = new byte[5];
        byteBuf.readBytes(fixBytes);
        Assert.assertEquals(fixBytes[4], 10);
    }

    public Request encodeRequestBody() {
        Request request = new RpcRequest();
        request.setProtocolName(StargateProtocol.PROTOCOL_NAME);
        request.setMethodName("method");
        request.setParams(new Object[] {"1313"});
        request.setParamsTypes(new Class[] {String.class});
        request.setServiceClass(StargateEncoder.class);
        request.setServiceConfig(new ServiceConfig());

        encoder.encodeBody(request);

        byte[] requestBytes = request.getBodyBytes();
        Assert.assertNotNull(requestBytes);
        Assert.assertTrue(request.getBodyBytes().length > 0);

        StargateRequest stargateRequest = (StargateRequest) serializer.deserialize(requestBytes, StargateRequest.class);
        Assert.assertNotNull(stargateRequest);
        Assert.assertEquals(stargateRequest.getMethodName(), "method");
        Assert.assertEquals(stargateRequest.getParameters()[0], "1313");
        Assert.assertEquals(stargateRequest.getUri().getParameter(Constants.GROUP_KEY), "normal");

        return request;
    }

    public Response encodeResponseBody() {

        Response response = new RpcResponse();
        response.setStatus(200);
        response.setResult("Test");
        response.setRequest(new RpcRequest());

        encoder.encodeBody(response);

        byte[] responseBytes = response.getBodyBytes();
        Assert.assertNotNull(responseBytes);
        StargateResponse stargateResponse =
            (StargateResponse) serializer.deserialize(responseBytes, StargateResponse.class);
        Assert.assertNotNull(stargateResponse);
        Assert.assertEquals(stargateResponse.getResult(), "Test");

        Assert.assertNotNull(response.getBodyBytes());
        Assert.assertTrue(response.getBodyBytes().length > 0);

        Response errResponse = new RpcResponse();
        errResponse.setStatus(StarlightRpcException.BIZ_ERROR);
        errResponse.setErrorMsg("Error");
        errResponse.setRequest(new RpcRequest());

        encoder.encodeBody(errResponse);

        byte[] errResponseBytes = errResponse.getBodyBytes();
        Assert.assertNotNull(errResponseBytes);
        StargateResponse errStargateResponse =
            (StargateResponse) serializer.deserialize(errResponseBytes, StargateResponse.class);
        Assert.assertNotNull(errStargateResponse);
        Assert.assertNotNull(errStargateResponse.getException());
        Assert.assertEquals(errStargateResponse.getException().getMessage(), "Error");

        return errResponse;
    }
}