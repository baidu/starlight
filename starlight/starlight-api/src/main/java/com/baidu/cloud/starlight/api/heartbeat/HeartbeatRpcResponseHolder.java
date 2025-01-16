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
 
package com.baidu.cloud.starlight.api.heartbeat;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;

/**
 * Created by liuruisen on 2021/4/23.
 */
public class HeartbeatRpcResponseHolder {

    public static RpcResponse heartbeatResponse(RpcRequest heartbeatRequest) {
        RpcResponse heartbeatResponse = new RpcResponse(heartbeatRequest.getId());
        heartbeatResponse.setRequest(heartbeatRequest);
        heartbeatResponse.setStatus(Constants.SUCCESS_CODE);
        heartbeatResponse.setReturnType(Heartbeat.class);
        heartbeatResponse.setResult(new Heartbeat(Constants.PONG));
        heartbeatResponse.setProtocolName(heartbeatRequest.getProtocolName());
        heartbeatResponse.setGenericReturnType(Heartbeat.class);

        return heartbeatResponse;
    }
}
