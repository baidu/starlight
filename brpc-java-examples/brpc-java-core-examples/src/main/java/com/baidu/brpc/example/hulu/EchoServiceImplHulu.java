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

package com.baidu.brpc.example.hulu;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.example.standard.Echo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoServiceImplHulu implements EchoServiceHulu {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServiceImplHulu.class);

    @Override
    public Echo.EchoResponse echo(Echo.EchoRequest request) {
        // 读取request attachment
        if (RpcContext.isSet()) {
            RpcContext rpcContext = RpcContext.getContext();
            String remoteHost = rpcContext.getRemoteHost();
            LOG.debug("remote host:{}", remoteHost);
            ByteBuf attachment = rpcContext.getRequestBinaryAttachment();
            if (attachment != null) {
                ByteBuf byteBuf = Unpooled.copiedBuffer(attachment);
                String attachmentString = new String(byteBuf.array());
                LOG.info("hulu received: request attachment=" + attachmentString);

                // 设置response attachment
                rpcContext.setResponseBinaryAttachment(byteBuf);
            }
        }

        String message = request.getMessage();
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                .setMessage(message).build();
        LOG.info("EchoService.echo, request={}, response={}",
                request.getMessage(), response.getMessage());

        return response;
    }

}
