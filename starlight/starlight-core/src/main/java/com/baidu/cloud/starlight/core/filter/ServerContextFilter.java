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
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol.X_STARLIGHT_ID;

/**
 * Created by liuruisen on 2020/9/2.
 */
public class ServerContextFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerContextFilter.class);

    @Override
    public void filterRequest(Invoker invoker, Request request, RpcCallback callback) {
        try {
            Object remoteHost = request.getAttachmentKv().remove(RpcContext.REMOTE_HOST_KEY);
            Object remotePort = request.getAttachmentKv().remove(RpcContext.REMOTE_PORT_KEY);
            RpcContext.getContext().setRemoteAddress(remoteHost instanceof String ? (String) remoteHost : null,
                remotePort instanceof Integer ? (Integer) remotePort : 0);

            RpcContext.getContext().set(request.getAttachmentKv());

            // original stargate will set params types and params to request attachment,
            // but is not used, we remove it to prevent encode/decode error
            if (RpcContext.getContext().get(RpcContext.PARMTYPES_KEY) != null) {
                RpcContext.getContext().remove(RpcContext.PARMTYPES_KEY);
            }
            if (RpcContext.getContext().get(RpcContext.PARMS_KEY) != null) {
                RpcContext.getContext().remove(RpcContext.PARMS_KEY);
            }
            if (RpcContext.getContext().get(RpcContext.METHODNAME_KEY) != null) {
                RpcContext.getContext().remove(RpcContext.METHODNAME_KEY);
            }
            // 删除生命周期为请求级别的Key，防止向下传递出现混乱
            RpcContext.getContext().remove(Constants.CONSUMER_APP_NAME_KEY);
            RpcContext.getContext().remove(Constants.PROVIDER_APP_NAME_KEY);
            RpcContext.getContext().remove(Constants.SERIALIZER_MODE_KEY);
            RpcContext.getContext().remove(Constants.REQUEST_TIMEOUT_KEY);
            RpcContext.getContext().remove(Constants.BEFORE_ENCODE_HEADER_TIME_KEY);
            RpcContext.getContext().remove(X_STARLIGHT_ID);
            LOGGER.debug("Server RpcContext values in thread {} is {}", Thread.currentThread().getName(),
                RpcContext.getContext().get());
        } catch (Throwable e) {
            LOGGER.warn("Set Request Attachment to RpcContext failed, {}", e.getMessage());
        }

        invoker.invoke(request, callback);
    }

    @Override
    public void filterResponse(Response response, Request request) {
        RpcContext.removeContext();
    }
}
