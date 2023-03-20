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

import com.baidu.cloud.starlight.api.utils.StringUtils;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NettyServletRequestAdaptor Since Spring-mvc {@link RequestMappingHandlerMapping} handle with HttpServletRequest, so
 * we convert {@link FullHttpRequest} to {@link HttpServletRequest}. Created by liuruisen on 2020/6/5.
 */
public class NettyServletRequestAdaptor implements HttpServletRequest {

    private FullHttpRequest nettyHttpRequest;

    private final String requestURI;

    private final String queryString;

    private final Map<String, String[]> params;

    private final Map<String, Object> attributes;

    private final ByteBufServletInputStream inputStream;

    private Channel channel;

    private Locale locale;

    public NettyServletRequestAdaptor(FullHttpRequest fullHttpRequest, Channel channel) {
        this(fullHttpRequest);
        this.channel = channel;
        this.locale = Locale.getDefault();
    }

    public NettyServletRequestAdaptor(FullHttpRequest fullHttpRequest) {
        this.nettyHttpRequest = fullHttpRequest;
        this.params = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
        if (nettyHttpRequest.uri().contains("?")) {
            String[] strs = nettyHttpRequest.uri().split("\\?");
            this.requestURI = strs[0];
            this.queryString = strs[1];
        } else {
            this.requestURI = nettyHttpRequest.uri();
            this.queryString = null;
        }

        if (!StringUtils.isBlank(queryString)) {
            String[] paramsStrs = queryString.split("&");
            for (String paramStr : paramsStrs) {
                String[] paramArr = paramStr.split("=");
                String key = paramArr[0];
                String value = paramArr[1];
                if (this.params.get(key) == null) {
                    this.params.put(key, new String[] {value});
                } else {
                    List<String> values = Arrays.asList(this.params.get(key));
                    values.add(value);
                    String[] valueArr = new String[] {};
                    values.toArray(valueArr);
                    params.put(key, valueArr);
                }
            }
        }

        byte[] requestBytes = new byte[nettyHttpRequest.content().readableBytes()];
        nettyHttpRequest.content().readBytes(requestBytes);
        inputStream = new ByteBufServletInputStream(new ByteBufInputStream(Unpooled.wrappedBuffer(requestBytes)));
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        String cookieString = nettyHttpRequest.headers().get(HttpHeaderNames.COOKIE);
        if (!StringUtils.isBlank(cookieString)) {
            Set<io.netty.handler.codec.http.cookie.Cookie> cookieSet = ServerCookieDecoder.LAX.decode(cookieString);
            if (cookieSet != null && cookieSet.size() > 0) {
                Cookie[] cookies = new Cookie[cookieSet.size()];
                int index = 0;
                for (io.netty.handler.codec.http.cookie.Cookie nettyCookie : cookieSet) {
                    Cookie cookie = new Cookie(nettyCookie.name(), nettyCookie.value());
                    cookie.setDomain(nettyCookie.domain() == null ? "" : nettyCookie.domain());
                    cookie.setMaxAge((int) nettyCookie.maxAge());
                    cookie.setPath(nettyCookie.path());
                    cookie.setHttpOnly(nettyCookie.isHttpOnly());
                    cookie.setSecure(nettyCookie.isSecure());
                    cookies[index] = cookie;
                    index++;
                }
                return cookies;
            }
        }
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return nettyHttpRequest.headers().get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Iterator<String> headersIterator = nettyHttpRequest.headers().getAll(name).iterator();
        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return headersIterator.hasNext();
            }

            @Override
            public String nextElement() {
                return headersIterator.next();
            }
        };
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Iterator<String> headerNamesIterator = nettyHttpRequest.headers().names().iterator();
        return new Enumeration<String>() {

            @Override
            public boolean hasMoreElements() {
                return headerNamesIterator.hasNext();
            }

            @Override
            public String nextElement() {
                return headerNamesIterator.next();
            }
        };
    }

    @Override
    public int getIntHeader(String name) {
        return Integer.parseInt(nettyHttpRequest.headers().get(name));
    }

    @Override
    public String getMethod() {
        return nettyHttpRequest.method().name();
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        String requestURI = getRequestURI();
        return "/".equals(requestURI) ? "" : requestURI;
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return this.requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer requestURLBuffer = new StringBuffer();
        requestURLBuffer.append(getScheme());
        requestURLBuffer.append("://");
        requestURLBuffer.append(getLocalAddr());
        requestURLBuffer.append(":");
        requestURLBuffer.append(getLocalPort());
        requestURLBuffer.append(getRequestURI());
        return requestURLBuffer;
    }

    @Override
    public String getServletPath() {
        return this.requestURI;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor Not support login current");
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor Not support logout current");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException { // multipart/form-data
        throw new UnsupportedOperationException("NettyServletRequestAdaptor Not support logout current");
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException { // multipart/form-data
        throw new UnsupportedOperationException("NettyServletRequestAdaptor Not support logout current");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // http1 upgrade to http2
        throw new UnsupportedOperationException("NettyServletRequestAdaptor Not support upgrade current");
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Iterator<String> iterator = attributes.keySet().iterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support set charset encoding");
    }

    @Override
    public int getContentLength() {
        return getIntHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
    }

    @Override
    public long getContentLengthLong() {
        return getIntHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
    }

    @Override
    public String getContentType() {
        return getHeader(HttpHeaderNames.CONTENT_TYPE.toString());
    }

    /**
     * Wrapper request body bytebuf as {@link ByteBufServletInputStream}
     *
     * @return
     * @throws IOException
     * @see ByteBufServletInputStream
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public String getParameter(String name) {
        return this.params.get(name) == null ? null : this.params.get(name)[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Iterator<String> iterator = params.keySet().iterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public String[] getParameterValues(String name) {
        return this.params.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return params;
    }

    @Override
    public String getProtocol() {
        return nettyHttpRequest.protocolVersion().protocolName();
    }

    @Override
    public String getScheme() {
        return nettyHttpRequest.protocolVersion().protocolName().toLowerCase();
    }

    @Override
    public String getServerName() {
        return getRemoteHost();
    }

    @Override
    public int getServerPort() {
        return getRemotePort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support getReader");
    }

    @Override
    public String getRemoteAddr() {
        String remoteAddr = getHeader("X-Forwarded-For"); // proxy forward
        if (StringUtils.isEmpty(remoteAddr)) {
            if (channel == null) {
                throw new UnsupportedOperationException(
                    "NettyServletRequestAdaptor not support to getRemoteHost " + "without channel");
            }
            InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
            remoteAddr = insocket.getAddress().getHostAddress();
        }
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        if (channel == null) {
            throw new UnsupportedOperationException(
                "NettyServletRequestAdaptor not support to getRemoteHost " + "without channel");
        }
        InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
        return insocket.getHostName();
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support to getLocales");
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support to getRequestDispatcher");
    }

    @Override
    public String getRealPath(String path) {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support to getRealPath");
    }

    @Override
    public int getRemotePort() {
        if (channel == null) {
            throw new UnsupportedOperationException(
                "NettyServletRequestAdaptor not support to getRemotePort " + "without channel");
        }
        InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
        return insocket.getPort();
    }

    @Override
    public String getLocalName() {
        if (channel == null) {
            throw new UnsupportedOperationException(
                "NettyServletRequestAdaptor not support to getLocalName " + "without channel");
        }
        InetSocketAddress insocket = (InetSocketAddress) channel.localAddress();
        return insocket.getHostName();
    }

    @Override
    public String getLocalAddr() {
        if (channel == null) {
            throw new UnsupportedOperationException(
                "NettyServletRequestAdaptor not support to getLocalAddr " + "without channel");
        }
        InetSocketAddress insocket = (InetSocketAddress) channel.localAddress();
        return insocket.getAddress().getHostAddress();
    }

    @Override
    public int getLocalPort() {
        if (channel == null) {
            throw new UnsupportedOperationException(
                "NettyServletRequestAdaptor not support to getLocalPort " + "without channel");
        }
        InetSocketAddress insocket = (InetSocketAddress) channel.localAddress();
        return insocket.getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support to startAsync");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IllegalStateException {
        throw new UnsupportedOperationException("NettyServletRequestAdaptor not support to startAsync");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    public FullHttpRequest getNettyHttpRequest() {
        return nettyHttpRequest;
    }

    public void setNettyHttpRequest(FullHttpRequest nettyHttpRequest) {
        this.nettyHttpRequest = nettyHttpRequest;
    }
}
