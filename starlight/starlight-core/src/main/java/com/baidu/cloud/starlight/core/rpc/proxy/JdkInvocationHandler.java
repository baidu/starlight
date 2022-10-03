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
 
package com.baidu.cloud.starlight.core.rpc.proxy;

import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.core.rpc.callback.BizWrapCallback;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.core.rpc.callback.FutureCallback;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Future;

public class JdkInvocationHandler implements InvocationHandler {

    private static final String FUTURE_SUFFIX = "Future";

    private static final String CALLBACK_SUFFIX = "Callback";

    private static final String ASYNC_INTERFACE_SUFFIX = "Async";

    private final StarlightClient client;

    private final Class<?> targetClass;

    private final ServiceConfig serviceConfig;

    JdkInvocationHandler(Class<?> targetClass, ServiceConfig serviceConfig, StarlightClient client) {
        this.client = client;
        this.targetClass = targetClass;
        this.serviceConfig = serviceConfig;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if ("equals".equals(method.getName()) && args.length == 1) {
            return equals(args[0]);
        } else if ("hashCode".equals(method.getName()) && (args == null || args.length == 0)) {
            return hashCode();
        } else if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
            return toString();
        }
        RpcCallback rpcCallback = null;
        Request request = null;
        Class<?> resultType = method.getReturnType(); // return type
        if (args != null && args.length > 1 && args[args.length - 1] instanceof Callback) {
            Callback bizCallBack = (Callback) args[args.length - 1];
            Object[] realArgs = Arrays.copyOf(args, args.length - 1);
            request = buildRequest(targetMethod(method), realArgs);
            rpcCallback = new BizWrapCallback(bizCallBack, request);
            client.request(request, rpcCallback);
            return null;
        } else {
            request = buildRequest(targetMethod(method), args);
            ResultFuture resultFuture = new ResultFuture();
            rpcCallback = new FutureCallback(resultFuture, request);
            client.request(request, rpcCallback);

            if (Future.class.isAssignableFrom(resultType)) {
                return resultFuture;
            }
            return resultFuture.get();
        }
    }

    private Request buildRequest(Method method, Object[] realArgs) {
        Request request = new RpcRequest();
        request.setServiceClass(targetServiceClass(targetClass));
        request.setMethodName(method.getName());
        request.setMethod(method);
        request.setParamsTypes(method.getParameterTypes());
        request.setGenericParamsTypes(method.getGenericParameterTypes());
        request.setParams(realArgs);
        request.setReturnType(method.getReturnType());
        request.setGenericReturnType(method.getGenericReturnType());
        request.setServiceConfig(serviceConfig);
        return request;
    }

    protected Method targetMethod(Method proxyMethod) {
        if (!isAsyncCall(proxyMethod)) {
            return proxyMethod;
        }

        Method targetMethod = null;
        Class<?>[] proxyParameterTypes = proxyMethod.getParameterTypes();
        Class<?> serviceClass = targetServiceClass(proxyMethod.getDeclaringClass());
        String methodName = proxyMethod.getName();

        // method end with Future
        if (methodName.endsWith(FUTURE_SUFFIX)) {
            try {
                targetMethod = serviceClass.getMethod(methodName.split(FUTURE_SUFFIX)[0], proxyParameterTypes);
            } catch (NoSuchMethodException e) {
                throw new StarlightRpcException(
                    "No related synchronization method in parent class, " + "async method { " + methodName + "}, "
                        + "related sync method {" + methodName.split("Future")[0] + "}");
            }
        }

        // method end with Future
        if (methodName.endsWith(CALLBACK_SUFFIX)) {
            try {
                proxyParameterTypes = Arrays.copyOf(proxyParameterTypes, proxyParameterTypes.length - 1);
                targetMethod = serviceClass.getMethod(methodName.split(CALLBACK_SUFFIX)[0], proxyParameterTypes);
            } catch (NoSuchMethodException e) {
                throw new StarlightRpcException(
                    "No related synchronization method in parent class, " + "async method { " + methodName + "}, "
                        + "related sync method {" + methodName.split("Callback")[0] + "}");
            }
        }
        return targetMethod;
    }

    protected Class<?> targetServiceClass(Class<?> proxyClass) {
        if (proxyClass.getInterfaces() != null && proxyClass.getInterfaces().length > 0) {
            if (proxyClass.getInterfaces().length > 1) {
                throw new StarlightRpcException("Starlight not support Multiple inheritance, "
                    + "Only supports one layer of inheritance when async call");
            }
            return proxyClass.getInterfaces()[0];
        } else {
            return proxyClass;
        }
    }

    /**
     * 判断本方法是否是异步方法
     * 
     * @param proxyMethod 未进行最终同步方法转换前的方法
     * @return
     */
    protected boolean isAsyncCall(Method proxyMethod) {

        if (targetClass.getInterfaces().length != 1) { // 接口继承层级为1
            return false;
        }

        if (!targetClass.getSimpleName().startsWith(ASYNC_INTERFACE_SUFFIX)) { // 接口前缀Async
            return false;
        }

        String methodName = proxyMethod.getName();
        if (!methodName.endsWith(CALLBACK_SUFFIX) && !methodName.endsWith(FUTURE_SUFFIX)) {
            return false;
        }

        // callback call, params has one callback
        if (methodName.endsWith(CALLBACK_SUFFIX)) {
            Class<?>[] paramTypes = proxyMethod.getParameterTypes();
            if (paramTypes.length == 0) {
                return false;
            }
            return Callback.class.isAssignableFrom(paramTypes[paramTypes.length - 1])
                && proxyMethod.getReturnType().equals(Void.TYPE);
        } else { // future
            return Future.class.isAssignableFrom(proxyMethod.getReturnType());
        }
    }
}