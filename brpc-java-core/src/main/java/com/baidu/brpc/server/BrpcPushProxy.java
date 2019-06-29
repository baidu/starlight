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

package com.baidu.brpc.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import com.baidu.brpc.JprotobufRpcMethodInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.DefaultInterceptorChain;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.interceptor.ServerPushInterceptor;
import com.baidu.brpc.protocol.Options;
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
public class BrpcPushProxy implements MethodInterceptor {

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

    private RpcServer rpcServer;
    private static Map<String, RpcMethodInfo> rpcMethodMap = new HashMap<String, RpcMethodInfo>();

    /**
     * 初始化时提前解析好method信息，在rpc交互时会更快。
     *
     * @param rpcServer rpcServer
     * @param clazz     rpc接口类
     */
    protected BrpcPushProxy(RpcServer rpcServer, Class clazz) {
        this.rpcServer = rpcServer;
        // 检查 serve push 协议
        if (!(rpcServer.getProtocol() instanceof ServerPushProtocol)) {
            throw new RpcException(" server protocol should be serverPushProtocl");
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (notProxyMethodSet.contains(method.getName())) {
                log.debug("{}:{} does not need to proxy",
                        method.getDeclaringClass().getName(), method.getName());
                continue;
            }

            Class<?>[] oriTypes = method.getParameterTypes();
            if (oriTypes.length < 1) {
                throw new IllegalArgumentException("number of arguments cannot be zero");
            } else if (!String.class.isAssignableFrom(oriTypes[0])) {
                throw new IllegalArgumentException("first arguments must be clientName (String)");
            } else if (Future.class.isAssignableFrom(method.getReturnType())
                    && !RpcCallback.class.isAssignableFrom(oriTypes[oriTypes.length - 1])) {
                throw new IllegalArgumentException("returnType is Future, but last argument is not RpcCallback");
            }
            // 转换为父接口 ， 去掉第一个clientName参数，不传到client端
            Class[] paramTypesExcludeClientName = ArrayUtils.subarray(oriTypes, 1, oriTypes.length);
            int paramLengthExcludeClientName = paramTypesExcludeClientName.length;

            Class[] actualArgTypes = paramTypesExcludeClientName;
            if (paramLengthExcludeClientName >= 1) { // 如果有callback，则去掉callback，否则跟去掉clientName后的参数一致
                if (RpcCallback.class.isAssignableFrom(paramTypesExcludeClientName[paramLengthExcludeClientName - 1])) {
                    actualArgTypes = ArrayUtils.subarray(paramTypesExcludeClientName, 0,
                            paramTypesExcludeClientName.length - 1);
                }
            }

            Method syncMethod;
            try {
                syncMethod = method.getDeclaringClass().getMethod(
                        method.getName(), actualArgTypes);
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException("can not find sync method:" + method.getName());
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

    public static <T> T getProxy(RpcServer rpcServer, Class clazz) {
        Enhancer en = new Enhancer();
        en.setSuperclass(clazz);
        en.setCallback(new BrpcPushProxy(rpcServer, clazz));
        return (T) en.create();
    }

    //    public BrpcPushProxy(RpcServer rpcServer) {
    //        this.rpcServer = rpcServer;
    //        timeoutTimer = new HashedWheelTimer(new CustomThreadFactory("timeout-timer-thread"));
    //
    //    }

    /**
     * server push 调用用户接口时候， 实际执行的方法
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
        Validate.notNull(rpcServer);
        String methodName = method.getName();
        RpcMethodInfo rpcMethodInfo = rpcMethodMap.get(methodName);
        if (rpcMethodInfo == null) {
            log.debug("{}:{} does not need to proxy",
                    method.getDeclaringClass().getName(), methodName);
            return proxy.invokeSuper(obj, args);
        }

        // 转换为父接口 ， 去掉第一个clientName参数，不传到client端
        Object[] actualArgs;
        int argLength = args.length;
        List<Object> argList = Arrays.asList(args);
        argList = argList.subList(1, argLength);
        argLength = argList.size();
        actualArgs = argList.toArray();

        Request request = null;
        Response response = null;

        List<Interceptor> interceptors = null;
        int readTimeout = 10 * 1000;
        int writeTimeout = 10 * 1000;
        // 区分 server端push还是client端发请求

        request = rpcServer.getProtocol().createRequest();
        response = rpcServer.getProtocol().getResponse();
        request.setClientName((String) args[0]);
        SPHead spHead = ((ServerPushProtocol) rpcServer.getProtocol()).createSPHead();
        spHead.setType(SPHead.TYPE_PUSH_REQUEST);
        request.setSpHead(spHead);
        request.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE.getNumber());
        interceptors = new ArrayList<Interceptor>();
        ServerPushInterceptor serverPushInterceptor = new ServerPushInterceptor();
        serverPushInterceptor.setRpcServer(rpcServer);
        interceptors.add(serverPushInterceptor);

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

            if (argLength > 1) {
                int startIndex = 0;
                int endIndex = argLength - 1;
                // 异步调用
                if (actualArgs[endIndex] instanceof RpcCallback) {
                    callback = (RpcCallback) actualArgs[endIndex];
                    endIndex -= 1;
                    argLength -= 1;
                }

                if (argLength <= 0) {
                    throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "invalid params");
                }

                Object[] sendArgs = new Object[argLength];
                for (int i = 0; startIndex <= endIndex; i++) {
                    sendArgs[i] = actualArgs[startIndex++];
                }
                request.setArgs(sendArgs);
                request.setCallback(callback);
            } else {
                // sync call
                request.setArgs(actualArgs);
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
                if (rpcContext.getNsHeadLogId() != null) {
                    request.getNsHead().logId = rpcContext.getNsHeadLogId();
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
