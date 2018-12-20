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

import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
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
        RpcResponse rpcResponse;
        if (!rpcClient.getRpcClientOptions().isHttp()) {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            try {
                rpcResponse = channelInfo.getProtocol().decodeResponse(packet, ctx);
            } catch (Exception ex) {
                log.warn("decode response failed:", ex);
                return;
            }
        } else {
            FullHttpResponse httpResponse = (FullHttpResponse) packet;
            Protocol protocol = rpcClient.getProtocol();
            rpcResponse = protocol.decodeHttpResponse(httpResponse, ctx);
        }

        if (rpcResponse.getRpcFuture() != null) {
            log.debug("handle response, logId={}", rpcResponse.getLogId());
            // 如果请求处理成功, 则在IO线程中触发用户回调方法;
            // 如果出错, 就变成了在非IO线程中触发用户回调方法;
            // 这种行为是否合适
            RpcFuture future = rpcResponse.getRpcFuture();
            future.setBinaryAttachment(rpcResponse.getBinaryAttachment());
            future.setKvAttachment(rpcResponse.getKvAttachment());
            future.handleResponse(rpcResponse);
        } else {
            log.debug("rpcFuture is null, logId={}", rpcResponse.getLogId());
        }
    }
}
