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
 
package com.baidu.cloud.starlight.protocol.http;

import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpMessage;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/6/2.
 */
public class HttpRequestResponseDecoderTest {

    private HttpRequestResponseDecoder requestResponseDecoder = new HttpRequestResponseDecoder();

    @Test
    public void createMessage() throws Exception {
        String[] responseInitLines = new String[] {"HTTP/1.1", "200", "OK"};
        HttpMessage httpMessage = requestResponseDecoder.createMessage(responseInitLines);

        Assert.assertTrue(httpMessage instanceof HttpResponse);
        Assert.assertTrue(((DefaultHttpResponse) httpMessage).status().code() == 200);
        Assert.assertFalse(requestResponseDecoder.isDecodingRequest());

        String[] requestInitLines = new String[] {"GET", "/echo", "HTTP/1.1"};
        HttpMessage httpMessage2 = requestResponseDecoder.createMessage(requestInitLines);
        Assert.assertTrue(httpMessage2 instanceof HttpRequest);
        Assert.assertTrue(((DefaultHttpRequest) httpMessage2).method().name().equals("GET"));
        Assert.assertTrue(requestResponseDecoder.isDecodingRequest());
    }

    @Test
    public void createInvalidMessage() throws Exception {
        // response
        String[] responseInitLines = new String[] {"HTTP/1.1", "200", "OK"};
        requestResponseDecoder.createMessage(responseInitLines);

        HttpMessage httpMessage = requestResponseDecoder.createInvalidMessage();
        Assert.assertTrue(httpMessage instanceof HttpResponse);
        Assert.assertTrue(((DefaultHttpResponse) httpMessage).status().code() == 999);

        // request
        String[] requestInitLines = new String[] {"GET", "/echo", "HTTP/1.1"};
        requestResponseDecoder.createMessage(requestInitLines);

        HttpMessage httpMessage2 = requestResponseDecoder.createInvalidMessage();
        Assert.assertTrue(httpMessage2 instanceof HttpRequest);
        Assert.assertTrue(((DefaultHttpRequest) httpMessage2).method().name().equals("GET"));
    }
}