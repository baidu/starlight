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
 
package com.baidu.cloud.starlight.transport.protocol.test;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.protocol.HeartbeatTrigger;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class TestHeartbeatTrigger implements HeartbeatTrigger {
    @Override
    public Request heartbeatRequest() {
        RpcRequest request = new RpcRequest();
        request.setProtocolName(ATestProtocol.PROTOCOL_NAME);
        request.setHeartbeat(true);
        request.setParams(new Object[] {"ping"});
        request.setParamsTypes(new Class[] {String.class});
        return request;
    }

    @Override
    public Response heartbeatResponse() {
        RpcResponse response = new RpcResponse();
        response.setProtocolName(ATestProtocol.PROTOCOL_NAME);
        response.setHeartbeat(true);
        response.setResult("pong");
        response.setStatus(Constants.SUCCESS_CODE);
        return response;
    }
}