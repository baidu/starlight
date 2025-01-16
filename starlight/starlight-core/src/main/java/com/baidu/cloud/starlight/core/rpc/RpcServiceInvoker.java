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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.sse.RpcSseEmitter;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by liuruisen on 2020/2/13.
 */
public class RpcServiceInvoker implements ServiceInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceInvoker.class);

    private RpcService rpcService;

    public RpcServiceInvoker(RpcService rpcService) {
        this.rpcService = rpcService;
    }

    @Override
    public RpcService getRpcService() {
        return rpcService;
    }

    @Override
    public void invoke(Request request, RpcCallback callback) {
        Object beforeServFilterExec = request.getNoneAdditionKv().get(Constants.BEFORE_SERVER_FILTER_EXEC_TIME_KEY);
        if (beforeServFilterExec instanceof Long) {
            LogUtils.addLogTimeAttachment(request, Constants.SERVER_FILTER_EXEC_COST_KEY,
                System.currentTimeMillis() - ((Long) beforeServFilterExec));
        }

        // get target method
        Method method = request.getMethod();
        if (method == null) {
            method = rpcService.getMethod(request.getMethodName());
            if (method == null) {
                callback.onError(new StarlightRpcException(StarlightRpcException.METHOD_NOT_FOUND_EXCEPTION,
                    "The called method {" + request.getMethodName() + "} does not exist"));
                return;
            }
        }

        // get service obj
        Object serviceObj = request.getServiceObj();
        if (serviceObj == null) {
            serviceObj = rpcService.getServiceObject();
        }

        Response response = new RpcResponse(request.getId());
        response.setProtocolName(request.getProtocolName());
        response.setRequest(request);

        // execute
        try {
            long beforeMethodExeTime = System.currentTimeMillis();
            LogUtils.addLogTimeAttachment(request, Constants.BEFORE_EXECUTE_METHOD_TIME_KEY, beforeMethodExeTime);
            Object result = method.invoke(serviceObj, request.getParams());
            LogUtils.addLogTimeAttachment(request, Constants.EXECUTE_METHOD_COST,
                System.currentTimeMillis() - beforeMethodExeTime);
            response.setStatus(Constants.SUCCESS_CODE);
            response.setResult(result);
            response.setReturnType(method.getReturnType());
            response.setGenericReturnType(method.getGenericReturnType());

            // init sseEmitter
            if (result instanceof RpcSseEmitter) {
                RpcSseEmitter rpcSseEmitter = (RpcSseEmitter) result;
                rpcSseEmitter.init(callback);
            } else {
                callback.onResponse(response);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to execute method " + request.getMethodName() + ", caused by ", e);
            callback.onError(convertThrowable(e));
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void init() {
        // do nothing
    }

    protected StarlightRpcException convertThrowable(Throwable throwable) {
        StringBuilder errMsgSb = new StringBuilder("Server failed to execute target method");
        errMsgSb.append(", cause by: ");

        if (throwable instanceof InvocationTargetException && throwable.getCause() != null) {
            errMsgSb.append(throwable.getCause().getClass().getSimpleName());
        } else {
            errMsgSb.append(throwable.getClass().getSimpleName());
        }

        errMsgSb.append(", ");

        if (throwable.getCause() != null) {
            errMsgSb
                .append(throwable.getMessage() == null ? throwable.getCause().getMessage() : throwable.getMessage());
        } else {
            errMsgSb.append(throwable.getMessage());
        }

        return new StarlightRpcException(StarlightRpcException.BIZ_ERROR, errMsgSb.toString());
    }
}
