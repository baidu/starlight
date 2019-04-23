/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.client.handler;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Response;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public class ClientWorkTask implements Runnable {
    private RpcClient rpcClient;
    private Object packet;
    private Protocol protocol;
    private ChannelHandlerContext ctx;

    @Override
    public void run() {
        Response response;

        try {
            response = protocol.decodeResponse(packet, ctx);
        } catch (Exception e) {
            log.warn("decode response failed:", e);
            return;
        }

        if (response.getRpcFuture() != null) {
            log.debug("handle response, logId={}", response.getLogId());
            RpcFuture future = response.getRpcFuture();
            future.handleResponse(response);
        } else {
            log.debug("rpcFuture is null, logId={}", response.getLogId());
        }
    }
}
