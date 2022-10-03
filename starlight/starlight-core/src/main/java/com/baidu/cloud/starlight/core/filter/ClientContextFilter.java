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
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.filter.Filter;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.utils.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Pass the parameters set by Consumer through RpcContext to Provider. Store some other context information for other
 * feature. Order = 1 Created by liuruisen on 2020/9/2.
 */
public class ClientContextFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientContextFilter.class);

    @Override
    public void filterRequest(Invoker invoker, Request request, RpcCallback callback) {

        // set remote address for easier access
        try {
            if (invoker instanceof ClientInvoker) {
                ClientInvoker clientInvoker = (ClientInvoker) invoker;
                URI remoteUri = clientInvoker.getClientPeer().getUri();
                RpcContext.getContext().setRemoteAddress(remoteUri.getHost(), remoteUri.getPort());
            }

            // set sessionID for easier access
            if (RpcContext.getContext().getSessionID() == null) {
                RpcContext.getContext().setSessionID(IdUtils.genUUID());
            }

            // set requestID for easier access
            if (RpcContext.getContext().getRequestID() == null) {
                RpcContext.getContext().setRequestID(String.valueOf(request.getId()));
            }

            // set rpc context values to request kv, will be passed to the provider
            if (RpcContext.getContext().get().size() > 0) {
                for (Map.Entry<String, Object> entry : RpcContext.getContext().get().entrySet()) {
                    // 兼容暴露springrest协议，http请求->stagate->server的场景(异常stackoverflowerror)
                    if (entry.getKey().equalsIgnoreCase(Constants.SERVLET_REQUEST_KEY)
                        || entry.getKey().equalsIgnoreCase(Constants.SERVLET_RESPONSE_KEY)) {
                        continue;
                    }
                    request.getAttachmentKv().putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
            // 跨线程释放
            request.getNoneAdditionKv().put("rpc_context", RpcContext.getContext());
            LOGGER.debug("Client RpcContext values in thread {} is {}", Thread.currentThread().getName(),
                RpcContext.getContext().get());
        } catch (Throwable e) {
            LOGGER.warn("Set RpcContext to Request Attachment failed, {}", e.getMessage());
        }
        invoker.invoke(request, callback);
    }

    @Override
    public void filterResponse(Response response, Request request) {
        // TODO 此处临时清理下rds_route_tag的内容，
        // 解决非StarlightServer，再使用StarlightClient调用后端场景不清除路由标识导致路由不符合预期，
        // 如请求以Http方式进入HttpServer，HttpServer中在以Stalright方式调用后端，当前HttpServer的调用线程中Context将会一直存有
        // TODO 后续可从拆分RpcContext为client side和server side 根解
        Object contextObj = request.getNoneAdditionKv().get("rpc_context");
        if (contextObj instanceof RpcContext) {
            RpcContext rpcContext = (RpcContext) contextObj;
            // 使用凤睛时会set，作为全链路传递的路由标识
            rpcContext.remove("rds_route_tag");
        }
    }
}