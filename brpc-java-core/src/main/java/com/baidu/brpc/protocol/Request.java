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
package com.baidu.brpc.protocol;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.naming.SubscribeInfo;
import com.baidu.brpc.protocol.nshead.NSHead;
import com.baidu.brpc.protocol.push.SPHead;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

public interface Request {

    /**
     * The msg param is the real request content to sent by netty.
     * For http protocols, the msg is an instance of {@link FullHttpRequest}.
     * For tcp protocols, the msg may be an instance of byte[].
     *
     * @see HttpRequest
     * @see RpcRequest
     */
    Object getMsg();

    void setMsg(Object o);

    long getLogId();

    void setLogId(long logId);

    Object getTarget();

    void setTarget(Object obj);

    Method getTargetMethod();

    void setTargetMethod(Method method);

    RpcMethodInfo getRpcMethodInfo();

    void setRpcMethodInfo(RpcMethodInfo rpcMethodInfo);

    String getServiceName();

    void setServiceName(String serviceName);

    String getMethodName();

    void setMethodName(String methodName);

    Object[] getArgs();

    void setArgs(Object[] newArgs);

    Map<String, Object> getKvAttachment();

    void setKvAttachment(Map<String, Object> requestKvAttachment);

    ByteBuf getBinaryAttachment();

    void setBinaryAttachment(ByteBuf requestBinaryAttachment);

    int getCompressType();

    void setCompressType(int number);

    RpcException getException();

    void setException(RpcException e);

    Channel getChannel();

    void setChannel(Channel channel);

    Set<BrpcChannel> getSelectedInstances();

    void setSelectedInstances(Set<BrpcChannel> selectedInstances);

    NSHead getNsHead();

    void setNsHead(NSHead nsHead);

    SPHead getSpHead();

    void setSpHead(SPHead spHead);

    Request retain();

    void release();

    void reset();

    String getAuth();

    void setAuth(String auth);

    Long getTraceId();

    void setTraceId(Long traceId);

    Long getSpanId();

    void setSpanId(Long spanId);

    Long getParentSpanId();

    void setParentSpanId(Long parentSpanId);

    RpcCallback getCallback();

    void setCallback(RpcCallback callback);

    String getServiceTag();

    void setServiceTag(String serviceTag);

    SubscribeInfo getSubscribeInfo();

    void setSubscribeInfo(SubscribeInfo subscribeInfo);

    Integer getReadTimeoutMillis();

    void setReadTimeoutMillis(Integer readTimeoutMillis);

    Integer getWriteTimeoutMillis();

    void setWriteTimeoutMillis(Integer writeTimeoutMillis);

    void setClientName(String clientName);

    String getClientName();

    boolean isOneWay();

    void setOneWay(boolean oneWay);
}
