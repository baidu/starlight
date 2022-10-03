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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultFullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpMethod;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol;
import com.baidu.cloud.starlight.protocol.http.User;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by liuruisen on 2020/6/30.
 */
public class SpringRestHttpDecoderTest {

    private static final SpringRestHttpDecoder springDecoder = new SpringRestHttpDecoder();

    private static FullHttpRequest getRequest;

    private static FullHttpRequest putRequest;

    private static FullHttpRequest deletRequest;

    private static FullHttpRequest postRequest;

    private static FullHttpRequest illegalHttpRequest;

    private static FullHttpRequest queryMapRequest;

    static {
        SpringRestHttpEncoder httpEncoder = new SpringRestHttpEncoder();
        RpcRequest rpcRequest = new RpcRequest(1);
        rpcRequest.setServiceClass(SpringRestService.class);
        rpcRequest.setProtocolName(SpringRestProtocol.PROTOCOL_NAME);

        // get http request
        try {
            rpcRequest.setMethod(SpringRestService.class.getMethod("get", String.class, String.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        rpcRequest.setMethodName("get");
        rpcRequest.setParams(new Object[] {"1", "123"});
        rpcRequest.setParamsTypes(new Class[] {String.class, String.class});

        getRequest = httpEncoder.convertRequest(rpcRequest);

        illegalHttpRequest = httpEncoder.convertRequest(rpcRequest);
        illegalHttpRequest.setMethod(HttpMethod.POST);

        try {
            rpcRequest.setMethod(SpringRestService.class.getMethod("getQueryMap", String.class, String.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        rpcRequest.setMethodName("getQueryMap");
        rpcRequest.setParams(new Object[] {"123", "456"});
        rpcRequest.setParamsTypes(new Class[] {String.class, String.class});
        queryMapRequest = httpEncoder.convertRequest(rpcRequest);

        User user = new User();
        user.setUserId(12);
        user.setName("test");
        // put http request
        rpcRequest.setMethodName("put");
        try {
            rpcRequest.setMethod(SpringRestService.class.getMethod("put", User.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        rpcRequest.setParams(new Object[] {user});
        rpcRequest.setParamsTypes(new Class[] {User.class});

        putRequest = httpEncoder.convertRequest(rpcRequest);

        // delete http request
        rpcRequest.setMethodName("delete");
        try {
            rpcRequest.setMethod(SpringRestService.class.getMethod("delete", String.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        rpcRequest.setParams(new Object[] {"1"});
        rpcRequest.setParamsTypes(new Class[] {String.class});

        deletRequest = httpEncoder.convertRequest(rpcRequest);

        // post http request
        rpcRequest.setMethodName("post");
        try {
            rpcRequest.setMethod(SpringRestService.class.getMethod("post", User.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        rpcRequest.setParams(new Object[] {user});
        rpcRequest.setParamsTypes(new Class[] {User.class});
        rpcRequest.setAttachmentKv(Collections.singletonMap("headerKey", "headerValue"));

        postRequest = httpEncoder.convertRequest(rpcRequest);

        SpringRestHandlerMapping handlerMapping = SpringRestHandlerMapping.getInstance();
        handlerMapping.createMapping(SpringRestService.class, new SpringRestService() {
            @Override
            public String get(String id, String query) {
                return null;
            }

            @Override
            public String put(User user) {
                return null;
            }

            @Override
            public String delete(String id) {
                return null;
            }

            @Override
            public String post(User user) {
                return null;
            }

            @Override
            public String getQueryMap(String query1, String query2) {
                return null;
            }
        });
    }

    @Test
    public void reverseConvertRequest() {
        JsonSerializer serializer = new JsonSerializer();

        // decode get request
        RpcRequest getRpcRequest = (RpcRequest) springDecoder.reverseConvertRequest(getRequest);
        Assert.assertNotNull(getRpcRequest);
        Assert.assertEquals(getRpcRequest.getMethodName(), "get");
        Assert.assertEquals(getRpcRequest.getServiceName(), SpringRestService.class.getName());
        Assert.assertNotNull(getRpcRequest);
        Assert.assertEquals(getRpcRequest.getParams().length, 2);
        Assert.assertEquals(getRpcRequest.getParams()[0], "1");

        // decode get query map request
        RpcRequest queryMapRpcRequest = (RpcRequest) springDecoder.reverseConvertRequest(queryMapRequest);
        Assert.assertNotNull(queryMapRpcRequest);
        Assert.assertEquals(queryMapRpcRequest.getMethodName(), "getQueryMap");
        Assert.assertEquals(queryMapRpcRequest.getServiceName(), SpringRestService.class.getName());
        Assert.assertNotNull(queryMapRpcRequest);
        Assert.assertEquals(queryMapRpcRequest.getParams().length, 2);
        Assert.assertEquals(queryMapRpcRequest.getParams()[0], "123");
        Assert.assertEquals(queryMapRpcRequest.getParams()[1], "456");

        // decode post request
        RpcRequest postRpcRequest = (RpcRequest) springDecoder.reverseConvertRequest(postRequest);
        Assert.assertNotNull(postRpcRequest);
        Assert.assertEquals(postRpcRequest.getMethodName(), "post");
        Assert.assertEquals(postRpcRequest.getServiceName(), SpringRestService.class.getName());
        Assert.assertNotNull(postRpcRequest);
        Assert.assertEquals(postRpcRequest.getParams().length, 1);
        Assert.assertTrue(postRpcRequest.getParams()[0] instanceof byte[]);
        User postUser = (User) serializer.deserialize((byte[]) postRpcRequest.getParams()[0], User.class);
        Assert.assertEquals(postUser.getName(), "test");

        // decode put request
        RpcRequest putRpcRequest = (RpcRequest) springDecoder.reverseConvertRequest(putRequest);
        Assert.assertNotNull(putRpcRequest);
        Assert.assertEquals(putRpcRequest.getMethodName(), "put");
        Assert.assertEquals(putRpcRequest.getServiceName(), SpringRestService.class.getName());
        Assert.assertNotNull(putRpcRequest);
        Assert.assertEquals(putRpcRequest.getParams().length, 1);
        Assert.assertTrue(postRpcRequest.getParams()[0] instanceof byte[]);
        User putUser = (User) serializer.deserialize((byte[]) postRpcRequest.getParams()[0], User.class);
        Assert.assertEquals(putUser.getName(), "test");

        // decode delete request
        RpcRequest deleteRpcRequest = (RpcRequest) springDecoder.reverseConvertRequest(deletRequest);
        Assert.assertNotNull(deleteRpcRequest);
        Assert.assertEquals(deleteRpcRequest.getMethodName(), "delete");
        Assert.assertEquals(deleteRpcRequest.getServiceName(), SpringRestService.class.getName());
        Assert.assertNotNull(deleteRpcRequest);
        Assert.assertEquals(deleteRpcRequest.getParams().length, 1);
        Assert.assertEquals(deleteRpcRequest.getParams()[0], "1");
    }

    @Test(expected = Exception.class)
    public void reverseConvertRequestError() {
        // reverseConvertResponseError
        springDecoder.reverseConvertRequest(illegalHttpRequest);

        illegalHttpRequest.setUri("/spring/1");
        springDecoder.reverseConvertRequest(illegalHttpRequest);
    }

    @Test
    public void reverseConvertResponse() {
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        httpResponse.headers().set(AbstractHttpProtocol.X_STARLIGHT_ID, 1l);
        Response response = springDecoder.reverseConvertResponse(httpResponse);
        Assert.assertEquals(response.getProtocolName(), SpringRestProtocol.PROTOCOL_NAME);
    }

}