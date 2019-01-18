/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

import com.baidu.brpc.JprotobufRpcMethodInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.protocol.RpcContext;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.utils.ProtobufUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
     * @param rpcClient rpc client对象
     * @param clazz rpc接口类
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
            Method syncMethod = method;
            if (parameterTypes.length > 1 && RpcCallback.class.isAssignableFrom(parameterTypes[0])) {
                // 异步方法
                Class[] actualParameterTypes = new Class[parameterTypes.length - 1];
                for (int i = 0; i < parameterTypes.length - 1; i++) {
                    actualParameterTypes[i] = parameterTypes[i];
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
        Enhancer en = new Enhancer();
        en.setSuperclass(clazz);
        en.setCallback(new BrpcProxy(rpcClient, clazz));
        return (T) en.create();
    }

    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
        String methodName = method.getName();
        RpcMethodInfo rpcMethodInfo = rpcMethodMap.get(methodName);
        if (rpcMethodInfo == null) {
            log.debug("{}:{} does not need to proxy",
                    method.getDeclaringClass().getName(), methodName);
            return proxy.invokeSuper(obj, args);
        }

        RpcRequest rpcRequest = null;
        RpcResponse rpcResponse = null;
        RpcContext rpcContext = RpcContext.getContext();
        try {
            rpcRequest = new RpcRequest();
            rpcRequest.setCompressType(rpcClient.getRpcClientOptions().getCompressType().getNumber());
            rpcRequest.setTarget(obj);
            rpcRequest.setRpcMethodInfo(rpcMethodInfo);
            rpcRequest.setTargetMethod(rpcMethodInfo.getMethod());
            rpcRequest.setServiceName(rpcMethodInfo.getServiceName());
            rpcRequest.setMethodName(rpcMethodInfo.getMethodName());
            rpcRequest.setNsHeadMeta(rpcMethodInfo.getNsHeadMeta());
            RpcCallback callback;
            int argLength = args.length;
            if (argLength > 1 && args[argLength - 1] instanceof RpcCallback) {
                // 异步调用
                argLength = argLength - 1;
                callback = (RpcCallback) args[argLength];
                Object[] newArgs = new Object[argLength];
                for (int i = 0; i < newArgs.length; i++) {
                    newArgs[i] = args[i];
                }
                rpcRequest.setArgs(newArgs);
            } else {
                // 同步调用
                callback = null;
                rpcRequest.setArgs(args);
            }

            // attachment
            rpcRequest.setKvAttachment(rpcContext.getRequestKvAttachment());
            rpcRequest.setBinaryAttachment(rpcContext.getRequestBinaryAttachment());

            // create and add RpcFuture object to FastFutureStore in order to acquire the logId,
            // which is required in interceptors;
            // The missing parameters will be set in rpcClient.sendRequest() method
            RpcFuture rpcFuture = new RpcFuture(null, rpcRequest.getRpcMethodInfo(), callback, null, null);
            long logId = FastFutureStore.getInstance(0).put(rpcFuture);
            rpcRequest.setLogId(logId);

            // 执行interceptor链
            if (CollectionUtils.isNotEmpty(rpcClient.getInterceptors())) {
                for (Interceptor interceptor : rpcClient.getInterceptors()) {
                    boolean success = interceptor.handleRequest(rpcRequest);
                    if (!success) {
                        log.warn("interceptor return false, terminate...");
                        throw new RpcException(RpcException.FORBIDDEN_EXCEPTION, "interceptor");
                    }
                }
            }

            Type responseType = rpcMethodInfo.getOutputClass();
            int currentTryTimes = 0;
            RpcException exception = null;
            Future future = null;
            while (currentTryTimes++ < rpcClient.getRpcClientOptions().getMaxTryTimes()) {
                boolean isFinalTry = currentTryTimes == rpcClient.getRpcClientOptions().getMaxTryTimes();

                try {
                    future = rpcClient.sendRequest(rpcRequest, responseType, callback, rpcFuture, isFinalTry);
                    if (callback != null) {
                        break;
                    } else {
                        rpcResponse = (RpcResponse) future.get(
                                rpcClient.getRpcClientOptions().getReadTimeoutMillis(),
                                TimeUnit.MILLISECONDS);
                        if (rpcResponse.getResult() != null) {
                            break;
                        } else {
                            // 同步异常时，需要release rpcResponse，然后再重试，防止被下一次rpcResponse覆盖。
                            rpcResponse.delRefCntForClient();
                        }
                    }
                } catch (RpcException ex) {
                    exception = ex;
                    if (isFinalTry) {
                        rpcClient.removeLogId(rpcRequest.getLogId());
                    }
                }
                // if application set the channel, brpc-java will not do retrying.
                // because application maybe send different request for different server instance.
                // this feature is used by Product Ads.
                if (rpcContext.getChannel() != null) {
                    break;
                }
            }
            if (rpcResponse == null) {
                rpcResponse = new RpcResponse();
                rpcResponse.setException(exception);
            }

            if (callback != null) {
                if (exception != null) {
                    throw exception;
                } else {
                    return future;
                }
            }

            // 执行interceptor链
            if (CollectionUtils.isNotEmpty(rpcClient.getInterceptors())) {
                int length = rpcClient.getInterceptors().size();
                for (int i = length - 1; i >= 0; i--) {
                    rpcClient.getInterceptors().get(i).handleResponse(rpcResponse);
                }
            }

            if (rpcResponse.getResult() != null) {
                return rpcResponse.getResult();
            } else {
                if (rpcResponse.getException() instanceof RpcException) {
                    RpcException rpcException = (RpcException) rpcResponse.getException();
                    throw rpcException;
                } else {
                    throw new RpcException(rpcResponse.getException());
                }
            }
        } finally {
            if (rpcRequest != null) {
                // 对于tcp协议，RpcRequest.refCnt可能会被retain多次，所以这里要减去当前refCnt。
                rpcRequest.delRefCnt();
            }
            if (rpcResponse != null) {
                rpcResponse.delRefCntForClient();
            }
            if (rpcContext != null) {
                rpcContext.reset();
            }
        }
    }

    public Map<String, RpcMethodInfo> getRpcMethodMap() {
        return rpcMethodMap;
    }
}
