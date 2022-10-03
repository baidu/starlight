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
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.servlet.ServletOutputStream;
import com.baidu.cloud.thirdparty.servlet.http.Cookie;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/10/19.
 */
public class NettyServletResponseAdaptorTest {

    @Test
    public void addCookie() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.SET_COOKIE.toString()));

        Cookie cookie = new Cookie("name", "test user");
        cookie.setComment("comment");
        cookie.setDomain("www.baidu.com");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(150);
        cookie.setPath("/test");
        cookie.setSecure(false);
        cookie.setVersion(1);

        responseAdaptor.addCookie(cookie);

        assertNotNull(responseAdaptor.getHeader(HttpHeaderNames.SET_COOKIE.toString()));
    }

    @Test
    public void containsHeader() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertFalse(responseAdaptor.containsHeader("test"));

        responseAdaptor.addHeader("test", "value");

        assertTrue(responseAdaptor.containsHeader("test"));

        assertEquals("value", responseAdaptor.getHeader("test"));
    }

    @Test
    public void encodeURL() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        String url = responseAdaptor.encodeURL("http://www.baidu.com/query=123");
        assertNotEquals("http://www.baidu.com/query=123", url);
    }

    @Test
    public void encodeRedirectURL() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        String url = responseAdaptor.encodeRedirectURL("http://www.baidu.com/query=123");
        assertNotEquals("http://www.baidu.com/query=123", url);
    }

    @Test
    public void encodeLowercaseUrl() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        String url = responseAdaptor.encodeUrl("http://www.baidu.com/query=123");
        assertNotEquals("http://www.baidu.com/query=123", url);
    }

    @Test
    public void encodeLowercaseRedirectUrl() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        String url = responseAdaptor.encodeRedirectUrl("http://www.baidu.com/query=123");
        assertNotEquals("http://www.baidu.com/query=123", url);
    }

    @Test
    public void sendError() throws IOException {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(200, responseAdaptor.getStatus());

        responseAdaptor.sendError(500);

        assertEquals(500, responseAdaptor.getStatus());
    }

    @Test
    public void testSendError() throws IOException {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(200, responseAdaptor.getStatus());

        responseAdaptor.sendError(500, "error");

        assertEquals(500, responseAdaptor.getStatus());

    }

    @Test
    public void sendRedirect() throws IOException {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.LOCATION.toString()));

        responseAdaptor.sendRedirect("www.baidu.com");

        assertEquals("www.baidu.com", responseAdaptor.getHeader(HttpHeaderNames.LOCATION.toString()));
    }

    @Test
    public void setDateHeader() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.DATE.toString()));

        Date oriDate = new Date();
        responseAdaptor.addDateHeader(HttpHeaderNames.DATE.toString(), oriDate.getTime());

        assertNotNull(responseAdaptor.getHeader(HttpHeaderNames.DATE.toString()));

        Date date = new Date();
        responseAdaptor.setDateHeader(HttpHeaderNames.DATE.toString(), date.getTime());

        assertNotEquals(oriDate.getTime(), responseAdaptor.getHeader(HttpHeaderNames.DATE.toString()));
    }

    @Test
    public void addDateHeader() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.DATE.toString()));

        Date oriDate = new Date();
        responseAdaptor.addDateHeader(HttpHeaderNames.DATE.toString(), oriDate.getTime());

        assertNotNull(responseAdaptor.getHeader(HttpHeaderNames.DATE.toString()));
    }

    @Test
    public void setHeader() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader("Test"));

        responseAdaptor.addHeader("Test", "v1");

        assertNotNull(responseAdaptor.getHeader("Test"));

        responseAdaptor.setHeader("Test", "v2");

        assertEquals("v2", responseAdaptor.getHeader("Test"));
    }

    @Test
    public void setIntHeader() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader("Test"));

        responseAdaptor.addIntHeader("Test", 1);

        assertNotNull(responseAdaptor.getHeader("Test"));

        responseAdaptor.setIntHeader("Test", 2);

        assertEquals(String.valueOf(2), responseAdaptor.getHeader("Test"));
    }

    @Test
    public void setStatus() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(200, responseAdaptor.getStatus());

        responseAdaptor.setStatus(600);

        assertEquals(600, responseAdaptor.getStatus());
    }

    @Test
    public void testSetStatus() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(200, responseAdaptor.getStatus());

        responseAdaptor.setStatus(600, "Error 600");

        assertEquals(600, responseAdaptor.getStatus());
    }

    @Test
    public void getStatus() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(200, responseAdaptor.getStatus());
    }

    @Test
    public void getHeaders() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader("Test"));

        responseAdaptor.setHeader("Test", "v1");
        assertNotNull(responseAdaptor.getHeader("Test"));

        assertEquals(1, responseAdaptor.getHeaders("Test").size());
    }

    @Test
    public void getHeaderNames() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(0, responseAdaptor.getHeaderNames().size());
        responseAdaptor.setHeader("Test", "v1");

        assertEquals(1, responseAdaptor.getHeaderNames().size());
    }

    @Test
    public void getCharacterEncoding() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals("utf-8", responseAdaptor.getCharacterEncoding());
    }

    @Test
    public void getContentType() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getContentType());

        responseAdaptor.setContentType("application/json");

        assertNotNull(responseAdaptor.getContentType());

        assertEquals("application/json", responseAdaptor.getContentType());

    }

    @Test
    public void getOutputStream() throws IOException {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNotNull(responseAdaptor.getOutputStream());

        ServletOutputStream outputStream = responseAdaptor.getOutputStream();

        assertFalse(outputStream.isReady());

        outputStream.write(1);

        assertTrue(outputStream.isReady());
    }

    @Test
    public void getWriter() throws IOException {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNotNull(responseAdaptor.getWriter());
    }

    @Test
    public void setCharacterEncoding() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals("utf-8", responseAdaptor.getCharacterEncoding());

        responseAdaptor.setCharacterEncoding("GBK");

        assertEquals("GBK", responseAdaptor.getCharacterEncoding());
    }

    @Test
    public void setContentLength() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString()));

        responseAdaptor.setContentLength(6666);

        assertEquals(Integer.valueOf(6666),
            Integer.valueOf(responseAdaptor.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString())));

    }

    @Test
    public void setContentLengthLong() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString()));

        responseAdaptor.setContentLengthLong(6666L);

        assertEquals(Integer.valueOf(6666),
            Integer.valueOf(responseAdaptor.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString())));
    }

    @Test
    public void setContentType() {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertNull(responseAdaptor.getHeader(HttpHeaderNames.CONTENT_TYPE.toString()));

        responseAdaptor.setContentType("text/html");

        assertEquals("text/html", responseAdaptor.getContentType());
    }

    @Test
    public void setBufferSize() {}

    @Test
    public void getBufferSize() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertEquals(0l, responseAdaptor.getBufferSize());
    }

    @Test
    public void flushBuffer() throws IOException {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        assertFalse(responseAdaptor.isCommitted());

        responseAdaptor.flushBuffer();

        assertTrue(responseAdaptor.isCommitted());
    }

    @Test
    public void resetBuffer() throws IOException {

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        responseAdaptor.getWriter().write(1);
        responseAdaptor.getWriter().flush();

        assertEquals(1, responseAdaptor.getBufferSize());

        responseAdaptor.resetBuffer();

        assertEquals(0, responseAdaptor.getBufferSize());
    }

    @Test
    public void reset() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        NettyServletResponseAdaptor responseAdaptor = new NettyServletResponseAdaptor(nettyResponse);

        responseAdaptor.addHeader("Test", "Value");
        responseAdaptor.reset();
        assertNull(responseAdaptor.getHeader("Test"));

        try {
            responseAdaptor.reset();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

    }
}