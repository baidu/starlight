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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.model.Wrapper;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.serialization.serializer.ProtoStuffSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class BrpcProtocolTest {

    private BrpcProtocol brpcProtocol = new BrpcProtocol();

    @Test
    public void getEncoder() {
        assertTrue(brpcProtocol.getEncoder() instanceof BrpcEncoder);
    }

    @Test
    public void getDecoder() {
        assertTrue(brpcProtocol.getDecoder() instanceof BrpcDecoder);
    }

    @Test
    public void getHeartbeatTrigger() {
        assertTrue(brpcProtocol.getHeartbeatTrigger() == null);
    }

    @Test
    public void getSerialize() {
        assertTrue(brpcProtocol.getSerialize(BrpcProtocol.SERIALIZER_TYPE_PROTOSTUFF) instanceof ProtoStuffSerializer);
    }

    @Test
    public void wrapRequest() {
        Request request = new RpcRequest();
        request.setAttachmentKv(new HashMap<>());
        request.setParamsTypes(new Class[] {TestModel.class});
        request.setParams(new Object[] {new TestModel()});

        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof TestModel);
        assertTrue(TestModel.class.isAssignableFrom(request.getParamsTypes()[0]));

        // multi params
        request.setParamsTypes(new Class[] {TestModel.class, String.class});
        request.setParams(new Object[] {new TestModel(), "test"});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(request.getParamsTypes()[0]));

        // Map
        request.setParamsTypes(new Class[] {Map.class});
        request.setParams(new Object[] {new HashMap<>()});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Map);
        assertTrue(Map.class.isAssignableFrom(request.getParamsTypes()[0]));

        // Collection
        request.setParamsTypes(new Class[] {ArrayList.class});
        request.setParams(new Object[] {new ArrayList<>()});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof List);
        assertTrue(List.class.isAssignableFrom(request.getParamsTypes()[0]));

        // Generic
        GenericUtil.markGeneric(request);
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(request.getParamsTypes()[0]));

        // serialize mode proto2-java
        request.getAttachmentKv().put(Constants.SERIALIZER_MODE_KEY, Constants.PROTO2_JAVA_MODE);

        request.setParamsTypes(new Class[] {TestModel.class});
        request.setParams(new Object[] {new TestModel()});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(request.getParamsTypes()[0]));

        // Map
        request.setParamsTypes(new Class[] {Map.class});
        request.setParams(new Object[] {new HashMap<>()});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(request.getParamsTypes()[0]));

        // Collection
        request.setParamsTypes(new Class[] {ArrayList.class});
        request.setParams(new Object[] {new ArrayList<>()});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(request.getParamsTypes()[0]));

        // 2020.0.1-SNAPSHOT场景，serializeMode为null，参数为单个或者多个
        request.getAttachmentKv().remove(Constants.SERIALIZER_MODE_KEY);
        request.getAttachmentKv().remove(Constants.IS_GENERIC_KEY);

        request.setParamsTypes(new Class[] {TestModel.class}); // 模拟2020.0.1的客户端请求参数
        request.setParams(new Object[] {new TestModel()});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof TestModel);
        assertTrue(TestModel.class.isAssignableFrom(request.getParamsTypes()[0]));

        request.setParamsTypes(new Class[] {String.class, Long.class, TestModel.class});
        request.setParams(new Object[] {"123", 123L, new TestModel()}); // 多参数
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(request.getParamsTypes()[0]));

        request.setParamsTypes(new Class[] {Long.class});
        request.setParams(new Object[] {123L});
        BrpcProtocol.wrapReqParams(request);
        assertTrue(request.getParams()[0] instanceof Long);
        assertTrue(Long.class.isAssignableFrom(request.getParamsTypes()[0]));
    }

    @Test
    public void wrapResponse() {
        Response response = new RpcResponse();
        response.setRequest(new RpcRequest());

        response.setStatus(Constants.SUCCESS_CODE);
        response.setReturnType(TestModel.class);
        response.setResult(new TestModel());

        // normal
        BrpcProtocol.wrapRespResult(response);
        assertTrue(response.getResult() instanceof TestModel);
        assertTrue(TestModel.class.isAssignableFrom(response.getReturnType()));

        // Collection
        response.setReturnType(ArrayList.class);
        response.setResult(new ArrayList<>());
        BrpcProtocol.wrapRespResult(response);
        assertTrue(response.getResult() instanceof List);
        assertTrue(List.class.isAssignableFrom(response.getReturnType()));

        // Map
        response.setReturnType(Map.class);
        response.setResult(new HashMap<>());
        BrpcProtocol.wrapRespResult(response);
        assertTrue(response.getResult() instanceof Map);
        assertTrue(Map.class.isAssignableFrom(response.getReturnType()));

        // Generic
        GenericUtil.markGeneric(response);
        BrpcProtocol.wrapRespResult(response);
        assertTrue(response.getResult() instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(response.getReturnType()));

        // proto2-java
        response.getAttachmentKv().remove(Constants.IS_GENERIC_KEY);

        Request request = new RpcRequest();
        request.getAttachmentKv().put(Constants.SERIALIZER_MODE_KEY, Constants.PROTO2_JAVA_MODE);
        response.setRequest(request);

        response.setReturnType(Map.class);
        response.setResult(new HashMap<>());
        BrpcProtocol.wrapRespResult(response);
        assertTrue(response.getResult() instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(response.getReturnType()));

        response.setReturnType(ArrayList.class);
        response.setResult(new ArrayList<>());
        BrpcProtocol.wrapRespResult(response);
        assertTrue(response.getResult() instanceof Wrapper);
        assertTrue(Wrapper.class.isAssignableFrom(response.getReturnType()));
    }
}