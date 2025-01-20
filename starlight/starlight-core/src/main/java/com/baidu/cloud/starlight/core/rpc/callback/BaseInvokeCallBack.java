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
 
package com.baidu.cloud.starlight.core.rpc.callback;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.RpcException;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.thirdparty.netty.util.Timeout;

/**
 * Server端 服务调用callback 基础类
 */
public class BaseInvokeCallBack implements RpcCallback {

    private Request request;

    private RpcChannel context;

    private Timeout timeout;

    public BaseInvokeCallBack(Request request, RpcChannel context) {
        this.request = request;
        this.context = context;
    }

    @Override
    public void onResponse(Response response) {
        if (timeout != null && !timeout.isCancelled()) {
            timeout.cancel();
        }

        response.setRequest(request);
        // serialize result body
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(request.getProtocolName());

        // encode body
        long beforeTime = System.currentTimeMillis();
        LogUtils.addLogTimeAttachment(response, Constants.BEFORE_ENCODE_BODY_TIME_KEY, beforeTime);
        protocol.getEncoder().encodeBody(response);
        LogUtils.addLogTimeAttachment(response, Constants.ENCODE_BODY_COST, System.currentTimeMillis() - beforeTime);

        // send msg
        LogUtils.addLogTimeAttachment(response, Constants.BEFORE_IO_THREAD_EXECUTE_TIME_KEY,
            System.currentTimeMillis());
        context.send(response);
    }

    @Override
    public void onError(Throwable e) {

        if (timeout != null && !timeout.isCancelled()) {
            timeout.cancel();
        }

        Response response = new RpcResponse(request.getId());
        response.setProtocolName(request.getProtocolName());
        if (e instanceof RpcException) {
            response.setStatus(((RpcException) e).getCode());
        } else {
            response.setStatus(StarlightRpcException.INTERNAL_SERVER_ERROR);
        }
        response.setErrorMsg(e.getMessage());
        response.setRequest(request); // store as context
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(request.getProtocolName());

        // encode body
        long beforeTime = System.currentTimeMillis();
        LogUtils.addLogTimeAttachment(response, Constants.BEFORE_ENCODE_BODY_TIME_KEY, beforeTime);
        protocol.getEncoder().encodeBody(response);
        LogUtils.addLogTimeAttachment(response, Constants.ENCODE_BODY_COST, System.currentTimeMillis() - beforeTime);

        // send msg
        LogUtils.addLogTimeAttachment(response, Constants.BEFORE_IO_THREAD_EXECUTE_TIME_KEY,
            System.currentTimeMillis());
        context.send(response);
    }

    @Override
    public void addTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    @Override
    public Request getRequest() {
        return request;
    }
}
