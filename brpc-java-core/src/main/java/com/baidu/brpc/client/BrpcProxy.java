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

package com.baidu.brpc.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.baidu.brpc.JprotobufRpcMethodInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.DefaultInterceptorChain;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.nshead.NSHead;
import com.baidu.brpc.protocol.nshead.NSHeadMeta;
import com.baidu.brpc.protocol.push.SPHead;
import com.baidu.brpc.protocol.push.ServerPushProtocol;
import com.baidu.brpc.utils.ProtobufUtils;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Created by huwenwei on 2017/4/25.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class BrpcProxy implements MethodInterceptor {

    private static final Set<String> notProxyMethodSet = new HashSet<String>();

    static {
        notProxyMethodSet.add("getClass");
        notProxyMethodSet.add("hashCode");
        notProxyMethodSet.add("equals");
        notProxyMethodSet.add("clone");
        notProxyMethodSet.add("toString");
        notProxyMethodSet.add("notify");
        notProxyMethodSet.add("notifyAll");
        notProxyMethodSet.add("wait");
        notProxyMethodSet.add("finalize");
    }

    private RpcClient rpcClient;

    private Map<String, RpcMethodInfo> rpcMethodMap = new HashMap<String, RpcMethodInfo>();

    /**
     * 初始化时提前解析好method信息，在rpc交互时会更快。
     *
     * @param rpcClient rpc client对象
     * @param clazz     rpc接口类
     */
    protected BrpcProxy(RpcClient rpcClient, Class clazz) {
        this.rpcClient = rpcClient;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (notProxyMethodSet.contains(method.getName())) {
                log.debug("{}:{} does not need to proxy",
                        method.getDeclaringClass().getName(), method.getName());
                continue;
            }

            Class[] parameterTypes = method.getParameterTypes();
            int paramLength = parameterTypes.length;
            if (paramLength >= 1
                    && Future.class.isAssignableFrom(method.getReturnType())
                    && !RpcCallback.class.isAssignableFrom(parameterTypes[paramLength - 1])) {
                throw new IllegalArgumentException("returnType is Future, but last argument is not RpcCallback");
            }

            Method syncMethod = method;
            if (paramLength > 1) {
                int startIndex = 0;
                int endIndex = paramLength - 1;
                // has callback, async rpc
                if (RpcCallback.class.isAssignableFrom(parameterTypes[paramLength - 1])) {
                    endIndex--;
                    paramLength--;
                }
                Class[] actualParameterTypes = new Class[paramLength];
                for (int i = 0; startIndex <= endIndex; i++) {
                    actualParameterTypes[i] = parameterTypes[startIndex++];
                }
                try {
                    syncMethod = method.getDeclaringClass().getMethod(
                            method.getName(), actualParameterTypes);
                } catch (NoSuchMethodException ex) {
                    throw new IllegalArgumentException("can not find sync method:" + method.getName());
                }
            }

            RpcMethodInfo methodInfo;
            ProtobufUtils.MessageType messageType = ProtobufUtils.getMessageType(syncMethod);
            if (messageType == ProtobufUtils.MessageType.PROTOBUF) {
                methodInfo = new ProtobufRpcMethodInfo(syncMethod);
            } else if (messageType == ProtobufUtils.MessageType.JPROTOBUF) {
                methodInfo = new JprotobufRpcMethodInfo(syncMethod);
            } else {
                methodInfo = new RpcMethodInfo(syncMethod);
            }

            rpcMethodMap.put(method.getName(), methodInfo);
            log.debug("client serviceName={}, methodName={}",
                    method.getDeclaringClass().getName(), method.getName());
        }
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz) {
        return getProxy(rpcClient, clazz, null);
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz, NamingOptions namingOptions) {
        rpcClient.setServiceInterface(clazz, namingOptions);
        rpcClient.getLoadBalanceInterceptor().setRpcClient(rpcClient);
        rpcClient.getInterceptors().add(rpcClient.getLoadBalanceInterceptor());
        Enhancer en = new Enhancer();
        en.setSuperclass(clazz);
        en.setCallback(new BrpcProxy(rpcClient, clazz));
        return (T) en.create();
    }

    /**
     * 调用用户接口时候， 实际执行的方法
     *
     * @param obj
     * @param method
     * @param args
     * @param proxy
     *
     * @return
     *
     * @throws Throwable
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
        String methodName = method.getName();
        RpcMethodInfo rpcMethodInfo = rpcMethodMap.get(methodName);
        if (rpcMethodInfo == null) {
            log.debug("{}:{} does not need to proxy",
                    method.getDeclaringClass().getName(), methodName);
            return proxy.invokeSuper(obj, args);
        }
        Request request = null;
        Response response = null;

        List<Interceptor> interceptors = null;
        int readTimeout = 10 * 1000;
        int writeTimeout = 10 * 1000;

        interceptors = rpcClient.getInterceptors();
        request = rpcClient.getProtocol().createRequest();
        response = rpcClient.getProtocol().getResponse();
        if (rpcClient.getProtocol() instanceof ServerPushProtocol) {
            SPHead spHead = ((ServerPushProtocol) rpcClient.getProtocol()).createSPHead();
            spHead.setType(SPHead.TYPE_REQUEST);
            request.setSpHead(spHead);
        }

        request.setCompressType(rpcClient.getRpcClientOptions().getCompressType().getNumber());
        request.setSubscribeInfo(rpcClient.getSubscribeInfo());
        readTimeout = rpcClient.getRpcClientOptions().getReadTimeoutMillis();
        writeTimeout = rpcClient.getRpcClientOptions().getWriteTimeoutMillis();

        try {

            request.setTarget(obj);
            request.setRpcMethodInfo(rpcMethodInfo);
            request.setTargetMethod(rpcMethodInfo.getMethod());
            request.setServiceName(rpcMethodInfo.getServiceName());
            request.setMethodName(rpcMethodInfo.getMethodName());
            NSHeadMeta nsHeadMeta = rpcMethodInfo.getNsHeadMeta();
            NSHead nsHead = nsHeadMeta == null ? new NSHead() : new NSHead(0, nsHeadMeta.id(), nsHeadMeta.version(),
                    nsHeadMeta.provider(), 0);
            request.setNsHead(nsHead);

            // parse request params
            RpcCallback callback = null;
            int argLength = args.length;
            if (argLength > 1) {
                int startIndex = 0;
                int endIndex = argLength - 1;
                // 异步调用
                if (args[endIndex] instanceof RpcCallback) {
                    callback = (RpcCallback) args[endIndex];
                    endIndex -= 1;
                    argLength -= 1;
                }

                if (argLength <= 0) {
                    throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "invalid params");
                }

                Object[] sendArgs = new Object[argLength];
                for (int i = 0; startIndex <= endIndex; i++) {
                    sendArgs[i] = args[startIndex++];
                }
                request.setArgs(sendArgs);
                request.setCallback(callback);
            } else {
                // sync call
                request.setArgs(args);
            }

            if (RpcContext.isSet()) {
                RpcContext rpcContext = RpcContext.getContext();
                // attachment
                if (rpcContext.getRequestKvAttachment() != null) {
                    request.setKvAttachment(rpcContext.getRequestKvAttachment());
                }
                if (rpcContext.getRequestBinaryAttachment() != null) {
                    request.setBinaryAttachment(rpcContext.getRequestBinaryAttachment());
                }
                if (rpcContext.getLogId() != null) {
                    request.getNsHead().logId = rpcContext.getLogId();
                    request.setLogId(rpcContext.getLogId());
                }
                if (rpcContext.getServiceTag() != null) {
                    request.setServiceTag(rpcContext.getServiceTag());
                }
                if (rpcContext.getReadTimeoutMillis() != null) {
                    request.setReadTimeoutMillis(rpcContext.getReadTimeoutMillis());
                }
                if (rpcContext.getWriteTimeoutMillis() != null) {
                    request.setWriteTimeoutMillis(rpcContext.getWriteTimeoutMillis());
                }
                rpcContext.reset();
            }

            if (request.getReadTimeoutMillis() == null) {
                request.setReadTimeoutMillis(readTimeout);
            }
            if (request.getWriteTimeoutMillis() == null) {
                request.setWriteTimeoutMillis(writeTimeout);
            }

            InterceptorChain interceptorChain = new DefaultInterceptorChain(interceptors);
            try {
                interceptorChain.intercept(request, response);
                if (response.getException() != null) {
                    throw new RpcException(response.getException());
                }
                if (request.getCallback() != null) {
                    return response.getRpcFuture();
                } else {
                    return response.getResult();
                }
            } catch (Exception ex) {
                log.error("exception :", ex);
                throw new RpcException(response.getException());
            }
        } finally {
            if (request != null) {
                // release send buffer because we retain send buffer when send request.
                request.release();
            }
        }
    }

    public Map<String, RpcMethodInfo> getRpcMethodMap() {
        return rpcMethodMap;
    }
}
