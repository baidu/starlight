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

import io.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.protocol.http.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by liuruisen on 2020/6/30.
 */
public class SpringRestHttpEncoderTest {

    private static final SpringRestHttpEncoder httpEncoder = new SpringRestHttpEncoder();

    @Test
    public void convertRequest() throws NoSuchMethodException {
        RpcRequest rpcRequest = new RpcRequest(1);
        rpcRequest.setServiceClass(SpringRestService.class);
        rpcRequest.setProtocolName(SpringRestProtocol.PROTOCOL_NAME);

        // get http request
        rpcRequest.setMethod(SpringRestService.class.getMethod("get", String.class, String.class));
        rpcRequest.setMethodName("get");
        rpcRequest.setParams(new Object[] {"1", "123"});
        rpcRequest.setParamsTypes(new Class[] {String.class, String.class});

        FullHttpRequest getRequest = httpEncoder.convertRequest(rpcRequest);
        Assert.assertNotNull(getRequest);
        Assert.assertNotNull(getRequest.uri());
        Assert.assertTrue(getRequest.uri().contains("/spring-rest/1"));
        Assert.assertTrue(getRequest.method().name().equalsIgnoreCase("get"));

        User user = new User();
        user.setUserId(12);
        user.setName("test");
        // put http request
        rpcRequest.setMethodName("put");
        rpcRequest.setMethod(SpringRestService.class.getMethod("put", User.class));
        rpcRequest.setParams(new Object[] {user});
        rpcRequest.setParamsTypes(new Class[] {User.class});

        FullHttpRequest putRequest = httpEncoder.convertRequest(rpcRequest);
        Assert.assertNotNull(putRequest);
        Assert.assertNotNull(putRequest.uri());
        Assert.assertTrue(putRequest.uri().contains("/spring-rest"));
        Assert.assertTrue(putRequest.method().name().equalsIgnoreCase("put"));
        Assert.assertNotNull(putRequest.content());

        // delete http request
        rpcRequest.setMethodName("delete");
        rpcRequest.setMethod(SpringRestService.class.getMethod("delete", String.class));
        rpcRequest.setParams(new Object[] {"1"});
        rpcRequest.setParamsTypes(new Class[] {String.class});

        FullHttpRequest deletRequest = httpEncoder.convertRequest(rpcRequest);
        Assert.assertNotNull(deletRequest);
        Assert.assertNotNull(deletRequest.uri());
        Assert.assertTrue(deletRequest.uri().contains("/spring-rest"));
        Assert.assertTrue(deletRequest.method().name().equalsIgnoreCase("delete"));

        // post http request
        rpcRequest.setMethodName("post");
        rpcRequest.setMethod(SpringRestService.class.getMethod("post", User.class));
        rpcRequest.setParams(new Object[] {user});
        rpcRequest.setParamsTypes(new Class[] {User.class});
        rpcRequest.setAttachmentKv(Collections.singletonMap("headerKey", "headerValue"));

        FullHttpRequest postRequest = httpEncoder.convertRequest(rpcRequest);
        Assert.assertNotNull(postRequest);
        Assert.assertNotNull(postRequest.uri());
        Assert.assertTrue(postRequest.uri().contains("/spring-rest"));
        Assert.assertTrue(postRequest.method().name().equalsIgnoreCase("post"));
        Assert.assertNotNull(postRequest.content());
        Assert.assertEquals(postRequest.headers().get("headerKey"), "headerValue");
    }

    @Test
    public void convertRequestError() throws NoSuchMethodException {
        RpcRequest rpcRequest = new RpcRequest(1);
        rpcRequest.setServiceClass(SpringRestService.class);
        rpcRequest.setProtocolName(SpringRestProtocol.PROTOCOL_NAME);

        // error method
        rpcRequest.setMethod(this.getClass().getMethod("convertRequestError"));
        try {
            httpEncoder.convertRequest(rpcRequest);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof CodecException);
        }
    }
}