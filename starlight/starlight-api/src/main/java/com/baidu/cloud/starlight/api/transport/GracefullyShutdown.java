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
 
package com.baidu.cloud.starlight.api.transport;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.model.ShuttingDownEvent;

/**
 * Created by liuruisen on 2020/11/27.
 */
public interface GracefullyShutdown {

    /**
     * In the process of graceful shutdown, a very small amount of traffic is allowed to enter during the quiet period.
     * After the quiet period, no traffic is allowed to enter. When the timeout period is reached or the task is
     * completed, exit
     * 
     * @param quietPeriod
     * @param timeout
     */
    void gracefullyShutdown(long quietPeriod, long timeout);

    /**
     * SHUTTING_DOWN_EVENT
     * 
     * @param protocol
     * @return
     */
    default RpcResponse shuttingDownEvent(String protocol) {
        RpcResponse shutdownEvent = new RpcResponse(0);
        shutdownEvent.setResult(new ShuttingDownEvent(System.currentTimeMillis()));
        shutdownEvent.setStatus(Constants.SUCCESS_CODE);
        shutdownEvent.setProtocolName(protocol);
        shutdownEvent.setReturnType(ShuttingDownEvent.class);

        return shutdownEvent;
    }

    /**
     * When is shutting down, response this
     * 
     * @param request
     * @return
     */
    default RpcResponse shuttingDownResponse(Request request) {
        RpcResponse response = new RpcResponse(request.getId());
        response.setRequest(request);
        response.setProtocolName(request.getProtocolName());
        response.setStatus(TransportException.SHUTTING_DOWN);
        response.setErrorMsg("The request will not be execute because it is shutting down");
        return response;
    }
}
