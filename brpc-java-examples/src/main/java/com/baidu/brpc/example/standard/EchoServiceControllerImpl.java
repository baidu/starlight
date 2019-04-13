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

package com.baidu.brpc.example.standard;

import com.baidu.brpc.Controller;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by huwenwei@baidu.com on 2019/4/8.
 */
public class EchoServiceControllerImpl implements EchoServiceController {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServiceControllerImpl.class);

    @Override
    public Echo.EchoResponse echo(Controller controller, Echo.EchoRequest request) {
        // 读取request attachment
        String remoteHost = controller.getRemoteHost();
        LOG.debug("remote host:{}", remoteHost);
        ByteBuf attachment = controller.getRequestBinaryAttachment();
        if (attachment != null) {
            if (LOG.isDebugEnabled()) {
                String attachmentString = new String(attachment.array());
                LOG.debug("request attachment={}", attachmentString);
            }
            // 设置response attachment
            controller.setResponseBinaryAttachment(Unpooled.copiedBuffer(attachment));
            attachment.release();
        }

        String message = request.getMessage();
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                .setMessage(message).build();
        LOG.debug("EchoService.echo, request={}, response={}",
                request.getMessage(), response.getMessage());

        return response;
    }
}
