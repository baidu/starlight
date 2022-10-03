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
 
package com.baidu.cloud.starlight.api.rpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.thirdparty.servlet.http.HttpServletRequest;
import com.baidu.cloud.thirdparty.servlet.http.HttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ThreadLocal based context Created by liuruisen on 2020/9/2.
 */
public class RpcContext {

    public static final String REMOTE_HOST_KEY = "remoteHost";
    public static final String REMOTE_PORT_KEY = "remotePort";
    public static final String PARMS_KEY = "parmeters";
    public static final String PARMTYPES_KEY = "parameterTypes";
    public static final String METHODNAME_KEY = "methodName";

    private static final ThreadLocal<RpcContext> RPC_CONTEXT = ThreadLocal.withInitial(RpcContext::new);

    private Map<String, Object> values = new LinkedHashMap<>();

    private Integer requestTimeoutMills;

    private String remoteHost;

    private Integer remotePort;

    public static RpcContext getContext() {
        return RPC_CONTEXT.get();
    }

    public static void removeContext() {
        RPC_CONTEXT.remove();
    }

    /**
     * Get ThreadLocal values
     *
     * @return
     */
    public Map<String, Object> get() {
        return values;
    }

    /**
     * Get ThreadLocal value
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    /**
     * Add k-v value
     *
     * @param key
     * @param value
     * @return
     */
    public RpcContext set(String key, Object value) {
        values.put(key, value);
        return this;
    }

    /**
     * Will override old values
     *
     * @param values
     * @return
     */
    public RpcContext set(Map<String, Object> values) {
        this.values.clear();
        if (values != null && values.size() > 0) {
            this.values.putAll(values);
        }
        return this;
    }

    public RpcContext remove(String key) {
        values.remove(key);
        return this;
    }

    /**
     * Get Session Id
     * 
     * @return
     */
    public String getSessionID() {
        return get(Constants.SESSION_ID_KEY);
    }

    /**
     * Set Session ID
     * 
     * @param sessionID
     */
    public void setSessionID(String sessionID) {
        set(Constants.SESSION_ID_KEY, sessionID);
    }

    /**
     * Get Request ID
     * 
     * @return
     */
    public String getRequestID() {
        return get(Constants.REQUEST_ID_KEY);
    }

    /**
     * Set Request ID
     * 
     * @param requestID
     */
    public void setRequestID(String requestID) {
        set(Constants.REQUEST_ID_KEY, requestID);
    }

    /**
     * Set requestTimeout
     * 
     * @param requestTimeoutMills
     */
    public void setRequestTimeoutMills(Integer requestTimeoutMills) {
        this.requestTimeoutMills = requestTimeoutMills;
    }

    public Integer getRequestTimeoutMills() {
        return this.requestTimeoutMills;
    }

    /**
     * set remote address.
     *
     * @param host
     * @param port
     * @return context
     */
    public RpcContext setRemoteAddress(String host, int port) {
        if (port < 0) {
            port = 0;
        }
        setRemoteHost(host);
        setRemotePort(port);
        return this;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    /**
     * get remote address string.
     *
     * @return remote address string.
     */
    public String getRemoteAddressString() {
        return getRemoteHostName() + ":" + getRemotePort();
    }

    /**
     * get remote host name.
     *
     * @return remote host name
     */
    public String getRemoteHostName() {
        return remoteHost;
    }

    /**
     * get remote port.
     *
     * @return remote port
     */
    public int getRemotePort() {
        return remotePort == null ? 0 : remotePort;
    }

    /**
     * get HttpServletRequest if has
     * 
     * @return
     */
    public HttpServletRequest getServletRequest() {
        return get(Constants.SERVLET_REQUEST_KEY);
    }

    /**
     * get HttpServletResponse if has
     * 
     * @return
     */
    public HttpServletResponse getServletResponse() {
        return get(Constants.SERVLET_RESPONSE_KEY);
    }
}
