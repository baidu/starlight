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

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/6/30.
 */
public class NettyServletRequestAdaptorTest {

    private static NettyServletRequestAdaptor adaptor;

    private static final int LENGTH = "TestTestTest".getBytes().length;

    private static final String PATH = "http://localhost:8080/spring-rest/post";

    private static final String QUERTSTRING = "query=123";

    static {
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
            PATH + "?" + QUERTSTRING, Unpooled.wrappedBuffer("TestTestTest".getBytes()));
        httpRequest.headers().add(HttpHeaderNames.COOKIE, "cookie1=cookie1;cookie2=cookie-cookie2");
        httpRequest.headers().add("HeaderKey", "TEST");
        httpRequest.headers().add("HeaderKeyInt", "123");
        httpRequest.headers().add(HttpHeaderNames.CONTENT_LENGTH, LENGTH);
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        adaptor = new NettyServletRequestAdaptor(httpRequest);
    }

    @Test
    public void getAuthType() {
        Assert.assertNull(adaptor.getAuthType());
    }

    @Test
    public void getCookies() {
        Cookie[] cookies = adaptor.getCookies();
        Assert.assertEquals(cookies.length, 2);
        Assert.assertEquals(cookies[0].getName(), "cookie1");
        Assert.assertEquals(cookies[0].getValue(), "cookie1");
    }

    @Test
    public void getDateHeader() {
        Assert.assertEquals(adaptor.getDateHeader("DataHeader"), 0);
    }

    @Test
    public void getHeader() {
        Assert.assertEquals(adaptor.getHeader("HeaderKey"), "TEST");
    }

    @Test
    public void getHeaders() {
        Enumeration<String> values = adaptor.getHeaders("HeaderKey");
        Assert.assertEquals(values.nextElement(), "TEST");
    }

    @Test
    public void getHeaderNames() {
        Enumeration<String> names = adaptor.getHeaderNames();
        List<String> nameList = new ArrayList<>();
        while (names.hasMoreElements()) {
            nameList.add(names.nextElement());
        }
        assertTrue(nameList.size() > 0);
        assertTrue(nameList.toString().contains("HeaderKey"));
    }

    @Test
    public void getIntHeader() {
        int value = adaptor.getIntHeader("HeaderKeyInt");
        Assert.assertEquals(value, 123);
    }

    @Test
    public void getMethod() {
        Assert.assertEquals(adaptor.getMethod(), HttpMethod.POST.name());
    }

    @Test
    public void getPathInfo() {
        Assert.assertNull(adaptor.getPathInfo());
    }

    @Test
    public void getPathTranslated() {
        Assert.assertNull(adaptor.getPathTranslated());
    }

    @Test
    public void getContextPath() {
        Assert.assertEquals(adaptor.getContextPath(), PATH);
    }

    @Test
    public void getQueryString() {
        Assert.assertEquals(adaptor.getQueryString(), QUERTSTRING);
    }

    @Test
    public void getRemoteUser() {
        Assert.assertNull(adaptor.getRemoteUser());
    }

    @Test
    public void isUserInRole() {
        Assert.assertFalse(adaptor.isUserInRole("null"));
    }

    @Test
    public void getUserPrincipal() {
        Assert.assertNull(adaptor.getUserPrincipal());
    }

    @Test
    public void getRequestedSessionId() {
        Assert.assertNull(adaptor.getRequestedSessionId());
    }

    @Test
    public void getRequestURI() {
        Assert.assertEquals(adaptor.getRequestURI(), PATH);
    }

    @Test
    public void getRequestURL() {
        try {
            adaptor.getRequestURL();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getServletPath() {
        Assert.assertEquals(adaptor.getServletPath(), PATH);

    }

    @Test
    public void getSession() {
        Assert.assertNull(adaptor.getSession());

    }

    @Test
    public void testGetSession() {
        Assert.assertNull(adaptor.getSession(true));
    }

    @Test
    public void changeSessionId() {
        Assert.assertNull(adaptor.changeSessionId());
    }

    @Test
    public void isRequestedSessionIdValid() {
        Assert.assertFalse(adaptor.isRequestedSessionIdValid());
    }

    @Test
    public void isRequestedSessionIdFromCookie() {
        Assert.assertFalse(adaptor.isRequestedSessionIdFromCookie());
    }

    @Test
    public void isRequestedSessionIdFromURL() {
        Assert.assertFalse(adaptor.isRequestedSessionIdFromURL());
    }

    @Test
    public void isRequestedSessionIdFromURLNew() {
        Assert.assertFalse(adaptor.isRequestedSessionIdFromUrl());
    }

    @Test
    public void authenticate() throws IOException, ServletException {
        Assert.assertFalse(adaptor.authenticate(null));
    }

    @Test
    public void login() {
        try {
            adaptor.login("user", "passw");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void logout() {
        try {
            adaptor.logout();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getParts() {
        try {
            adaptor.getParts();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getPart() {
        try {
            adaptor.getPart("name");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void upgrade() {
        try {
            adaptor.upgrade(null);
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getAttribute() {
        Assert.assertNull(adaptor.getAttribute("attr1"));
        adaptor.setAttribute("attr1", "att1");
        adaptor.setAttribute("attr2", "att2");

        Assert.assertEquals(adaptor.getAttribute("attr1"), "att1");

        Enumeration<String> names = adaptor.getAttributeNames();
        List<String> nameList = new ArrayList<>();
        while (names.hasMoreElements()) {
            nameList.add(names.nextElement());
        }
        assertTrue(nameList.size() > 0);
        assertTrue(nameList.toString().contains("attr1"));
        assertTrue(nameList.toString().contains("attr2"));

        adaptor.removeAttribute("attr2");
        Assert.assertNull(adaptor.getAttribute("attr2"));
    }

    @Test
    public void getCharacterEncoding() {
        Assert.assertEquals(adaptor.getCharacterEncoding(), "UTF-8");
    }

    @Test
    public void setCharacterEncoding() {
        try {
            adaptor.setCharacterEncoding(null);
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getContentLength() {
        Assert.assertEquals(adaptor.getContentLength(), LENGTH);
    }

    @Test
    public void getContentLengthLong() {
        Assert.assertEquals(adaptor.getContentLength(), LENGTH);
    }

    @Test
    public void getContentType() {
        Assert.assertEquals(adaptor.getContentType(), HttpHeaderValues.APPLICATION_JSON.toString());
    }

    @Test
    public void getInputStream() throws IOException {
        ServletInputStream servletInputStream = adaptor.getInputStream();
        assertTrue(servletInputStream instanceof ByteBufServletInputStream);
        assertTrue((servletInputStream).isReady());
    }

    @Test
    public void getParameter() {
        Assert.assertEquals(adaptor.getParameter("query"), "123");
    }

    @Test
    public void getParameterNames() {
        Enumeration<String> names = adaptor.getParameterNames();
        List<String> nameList = new ArrayList<>();
        while (names.hasMoreElements()) {
            nameList.add(names.nextElement());
        }
        assertTrue(nameList.size() > 0);
        assertTrue(nameList.toString().contains("query"));
    }

    @Test
    public void getParameterValues() {
        String[] values = adaptor.getParameterValues("query");
        Assert.assertEquals(values.length, 1);
        Assert.assertEquals(values[0], "123");
    }

    @Test
    public void getParameterMap() {
        Map<String, String[]> parameterMap = adaptor.getParameterMap();
        Assert.assertEquals(parameterMap.get("query")[0], "123");
    }

    @Test
    public void getProtocol() {
        Assert.assertEquals(adaptor.getProtocol(), HttpVersion.HTTP_1_1.protocolName());
    }

    @Test
    public void getScheme() {
        Assert.assertEquals(adaptor.getScheme(), HttpVersion.HTTP_1_1.protocolName().toLowerCase());
    }

    @Test
    public void getServerName() {
        try {
            adaptor.getServerName();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getServerPort() {
        try {
            adaptor.getServerPort();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getReader() {
        try {
            adaptor.getReader();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getRemoteAddr() {
        try {
            adaptor.getRemoteAddr();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getRemoteHost() {
        try {
            adaptor.getRemoteHost();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getLocale() {
        try {
            adaptor.getLocale();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getLocales() {
        try {
            adaptor.getLocales();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void isSecure() {
        Assert.assertFalse(adaptor.isSecure());
    }

    @Test
    public void getRequestDispatcher() {
        try {
            adaptor.getRequestDispatcher("/spring-rest");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getRealPath() {
        try {
            adaptor.getRealPath("/spring-rest");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getRemotePort() {
        try {
            adaptor.getRemotePort();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getLocalName() {
        try {
            adaptor.getLocalName();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getLocalAddr() {
        try {
            adaptor.getLocalAddr();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getLocalPort() {
        try {
            adaptor.getLocalPort();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getServletContext() {
        Assert.assertNull(adaptor.getServletContext());
    }

    @Test
    public void startAsync() {
        try {
            adaptor.startAsync();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testStartAsync() {
        try {
            adaptor.startAsync(null, null);
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void isAsyncStarted() {
        Assert.assertFalse(adaptor.isAsyncStarted());
    }

    @Test
    public void isAsyncSupported() {
        Assert.assertFalse(adaptor.isAsyncSupported());
    }

    @Test
    public void getAsyncContext() {
        Assert.assertNull(adaptor.getAsyncContext());
    }

    @Test
    public void getDispatcherType() {
        Assert.assertNull(adaptor.getDispatcherType());
    }
}