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
 
package com.baidu.cloud.starlight.core.filter;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.filter.Filter;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.core.rpc.RpcServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.core.utils.PojoJsonUtils;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Only used in server side Compatible with stargate framework Created by liuruisen on 2020/4/7.
 */
public class GenericFilter implements Filter {

    @Override
    public void filterRequest(Invoker invoker, Request request, RpcCallback callback) {
        if (GenericUtil.isGenericCall(request) // method name contains $invoke
            && request.getParams() != null && request.getParams().length == 2) { // methodName + Object[]

            GenericUtil.markGeneric(request); // mark request generic

            Object[] params = request.getParams();
            String methodName = ((String) params[0]).trim();
            request.setMethodName(methodName); // methodName

            Object[] args = (Object[]) params[1]; // real args
            // get method
            ServiceInvoker serviceInvoker = RpcServiceRegistry.getInstance().discover(request.getServiceName());
            if (serviceInvoker == null) {
                callback.onError(new StarlightRpcException(StarlightRpcException.SERVICE_NOT_FOUND_EXCEPTION,
                    "Service {" + request.getServiceName() + "} not found in provider"));
                return;
            }
            Method method = serviceInvoker.getRpcService().getMethod(methodName);
            if (method == null) {
                callback.onError(new StarlightRpcException(StarlightRpcException.METHOD_NOT_FOUND_EXCEPTION,
                    "The called method {" + request.getMethodName() + "} does not exist"));
                return;
            }
            Type[] paramTypes = method.getGenericParameterTypes();

            try {
                args = PojoJsonUtils.realize(args, paramTypes); // realize params
            } catch (Exception e) {
                callback.onError(new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                    "Generic request exception, convert generic args to real args failed, error: " + e.getMessage()));
                return;
            }
            request.setParams(args);
        } else {
            RpcContext.getContext().remove(Constants.IS_GENERIC_KEY);
            if (request.getAttachmentKv() != null) { // not to be a generalization request, remove is_generic attach
                request.getAttachmentKv().remove(Constants.IS_GENERIC_KEY);
            }
        }
        invoker.invoke(request, callback);
    }

    @Override
    public void filterResponse(Response response, Request request) {
        try {
            if (request.getAttachmentKv() != null && request.getAttachmentKv().get(Constants.IS_GENERIC_KEY) != null
                && (Boolean) request.getAttachmentKv().get(Constants.IS_GENERIC_KEY)) {

                GenericUtil.markGeneric(response);

                Object result = response.getResult();
                response.setResult(PojoJsonUtils.generalize(result));
            }
        } catch (Exception e) {
            response.setResult(null);
            response.setErrorMsg(
                "Generic response exception, convert real result to generic result failed, error: " + e.getMessage());
            response.setStatus(StarlightRpcException.INTERNAL_SERVER_ERROR);
        }
    }

}
