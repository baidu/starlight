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
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.utils.IDGenerator;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.starlight.protocol.stargate.StargateProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client Invoker Used to serialize body and request with {@link ClientPeer} Created by liuruisen on 2020/2/14.
 */
public class RpcClientInvoker implements ClientInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientInvoker.class);

    private final ClientPeer clientPeer;

    private final ServiceConfig serviceConfig;

    public RpcClientInvoker(ClientPeer clientPeer, ServiceConfig serviceConfig) {
        this.clientPeer = clientPeer;
        this.serviceConfig = serviceConfig;
    }

    @Override
    public ClientPeer getClientPeer() {
        return clientPeer;
    }

    @Override
    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    @Override
    public void invoke(Request request, RpcCallback callback) {
        String protocolName = request.getProtocolName();
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(protocolName);
        // 当请求协议为stargate时，生成更具唯一性的ID
        changeIdForStargate(request);
        // body encode: Construct and Serialize
        try {
            long beforeTime = System.currentTimeMillis();
            LogUtils.addLogTimeAttachment(request, Constants.BEFORE_ENCODE_BODY_TIME_KEY, beforeTime);
            protocol.getEncoder().encodeBody(request);
            LogUtils.addLogTimeAttachment(request, Constants.ENCODE_BODY_COST, System.currentTimeMillis() - beforeTime);
        } catch (CodecException e) {
            callback.onError(e);
            return;
        }

        // request
        try {
            request.getNoneAdditionKv().put(Constants.REMOTE_ADDRESS_KEY,
                clientPeer.getUri() != null ? clientPeer.getUri().getAddress() : null);

            request.getNoneAdditionKv().put(Constants.BEFORE_IO_THREAD_EXECUTE_TIME_KEY, System.currentTimeMillis());

            // send msg
            clientPeer.request(request, callback);
        } catch (TransportException e) {
            // call back error
            callback.onError(e);
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

    private void changeIdForStargate(Request request) {
        try {
            String protocolName = request.getProtocolName();
            if (protocolName.equals(StargateProtocol.PROTOCOL_NAME)) {
                request.setId(IDGenerator.getInstance().nextId());
            }
        } catch (Throwable e) {
            LOGGER.warn("Generate stargate requestId form IDGenerator failed, caused by ", e);
        }
    }
}
