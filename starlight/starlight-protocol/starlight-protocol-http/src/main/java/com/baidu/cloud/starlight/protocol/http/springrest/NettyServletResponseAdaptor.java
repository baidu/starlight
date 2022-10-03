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

import com.baidu.cloud.thirdparty.netty.buffer.ByteBufOutputStream;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.cookie.DefaultCookie;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.cookie.ServerCookieEncoder;
import com.baidu.cloud.thirdparty.servlet.ServletException;
import com.baidu.cloud.thirdparty.servlet.ServletOutputStream;
import com.baidu.cloud.thirdparty.servlet.http.Cookie;
import com.baidu.cloud.thirdparty.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

/**
 * NettyServletRequestAdaptor Since Spring-mvc {@link SpringRestHandlerMapping} handle with HttpServletRequest, so we
 * convert {@link FullHttpResponse} to {@link HttpServletResponse}. Created by liuruisen on 2020/10/16.
 */
public class NettyServletResponseAdaptor implements HttpServletResponse {

    private NettyServletRequestAdaptor servletRequest;

    private FullHttpResponse nettyHttpResponse;

    private ByteBufServletOutputStream outputStream;

    private Locale locale;

    private boolean commited;

    private PrintWriter writer;

    public NettyServletResponseAdaptor(FullHttpResponse nettyHttpResponse, NettyServletRequestAdaptor servletRequest) {
        this(nettyHttpResponse);
        this.servletRequest = servletRequest;
    }

    public NettyServletResponseAdaptor(FullHttpResponse nettyResponse) {
        this.nettyHttpResponse = nettyResponse;
        this.outputStream = new ByteBufServletOutputStream(new ByteBufOutputStream(nettyResponse.content()));
        this.writer = new PrintWriter(outputStream);
        this.locale = Locale.getDefault();
    }

    @Override
    public void addCookie(Cookie cookie) {
        DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
        nettyCookie.setDomain(cookie.getDomain());
        nettyCookie.setMaxAge(cookie.getMaxAge());
        nettyCookie.setPath(cookie.getPath());
        nettyCookie.setHttpOnly(cookie.isHttpOnly());
        nettyCookie.setSecure(cookie.getSecure());
        String cookResult = ServerCookieEncoder.LAX.encode(nettyCookie);
        nettyHttpResponse.headers().add(HttpHeaderNames.SET_COOKIE, cookResult);
    }

    @Override
    public boolean containsHeader(String headerKey) {
        return nettyHttpResponse.headers().contains(headerKey);
    }

    @Override
    public String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(new ServletException("Encoder URL failed", e));
        }
    }

    @Override
    public String encodeRedirectURL(String url) {
        return this.encodeURL(url);
    }

    @Override
    public String encodeUrl(String url) {
        return this.encodeURL(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return this.encodeURL(url);
    }

    @Override
    public void sendError(int code, String msg) throws IOException {
        if (msg != null) {
            msg = msg.replace("\r", " ");
            msg = msg.replace("\n", " ");
        }

        this.nettyHttpResponse.setStatus(new HttpResponseStatus(code, msg));
    }

    @Override
    public void sendError(int status) throws IOException {
        // FIXME simple implement 需参考其他实现，是否有更优解
        this.nettyHttpResponse.setStatus(HttpResponseStatus.valueOf(status));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        // FIXME simple implement 需参考其他实现，是否有更优解
        setStatus(SC_FOUND);
        setHeader(HttpHeaderNames.LOCATION.toString(), location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        this.nettyHttpResponse.headers().set(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        this.nettyHttpResponse.headers().add(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        this.nettyHttpResponse.headers().set(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        this.nettyHttpResponse.headers().add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        this.nettyHttpResponse.headers().setInt(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        this.nettyHttpResponse.headers().addInt(name, value);
    }

    @Override
    public void setStatus(int status) {
        this.nettyHttpResponse.setStatus(HttpResponseStatus.valueOf(status));
    }

    @Override
    public void setStatus(int status, String msg) {
        this.nettyHttpResponse.setStatus(new HttpResponseStatus(status, msg));
    }

    @Override
    public int getStatus() {
        return nettyHttpResponse.status().code();
    }

    @Override
    public String getHeader(String name) {
        return nettyHttpResponse.headers().get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Arrays.asList(nettyHttpResponse.headers().get(name));
    }

    @Override
    public Collection<String> getHeaderNames() {
        return nettyHttpResponse.headers().names();
    }

    @Override
    public String getCharacterEncoding() {
        String charset = nettyHttpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING);
        return charset != null ? charset : "utf-8";
    }

    @Override
    public String getContentType() {
        return nettyHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return this.writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        nettyHttpResponse.headers().set(HttpHeaderNames.CONTENT_ENCODING, charset);
    }

    @Override
    public void setContentLength(int contentLength) {
        nettyHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
    }

    @Override
    public void setContentLengthLong(long contentLengthLong) {
        nettyHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLengthLong);
    }

    @Override
    public void setContentType(String contentType) {
        nettyHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    @Override
    public void setBufferSize(int bufferSize) {

    }

    @Override
    public int getBufferSize() {
        return this.outputStream.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        this.getWriter().flush();
        this.commited = true;
    }

    @Override
    public void resetBuffer() {
        this.outputStream.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return commited;
    }

    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed!");
        }
        this.nettyHttpResponse.headers().clear();
        this.resetBuffer();
    }

    @Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public FullHttpResponse getNettyHttpResponse() {
        return nettyHttpResponse;
    }

    public void setNettyHttpResponse(FullHttpResponse nettyHttpResponse) {
        this.nettyHttpResponse = nettyHttpResponse;
    }

    public NettyServletRequestAdaptor getServletRequest() {
        return servletRequest;
    }

    public void setServletRequest(NettyServletRequestAdaptor servletRequest) {
        this.servletRequest = servletRequest;
    }
}
