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

package com.baidu.brpc.example.spring.server;

import com.baidu.brpc.example.spring.api.EchoRequest;
import com.baidu.brpc.example.spring.api.EchoResponse;
import com.baidu.brpc.example.spring.api.EchoService;
import com.baidu.brpc.spring.annotation.NamingOption;
import com.baidu.brpc.spring.annotation.RpcExporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("echoServiceImpl")
@RpcExporter(port = "8012",
        useSharedThreadPool = false,
        rpcServerOptionsBeanName = "rpcServerOptions",
        interceptorBeanName = "customInterceptor",
        extraOptions = {
                // We can pass extra options to the NamingService
                // `weight` is just an example here, currently we don't have any NamingService supports weight yet
                @NamingOption(key = "weight", value = "10")
        }
)
public class EchoServiceImpl implements EchoService {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServiceImpl.class);

    @Override
    public EchoResponse echo(EchoRequest request) {
        // 读取request attachment
//        RpcContext rpcContext = RpcContext.getContext();
//        ByteBuf attachment = rpcContext.getRequestBinaryAttachment();
//        if (attachment != null) {
//            if (LOG.isDebugEnabled()) {
//                String attachmentString = new String(attachment.array());
//                LOG.debug("request attachment={}", attachmentString);
//            }
//            // 设置response attachment
//            rpcContext.setResponseBinaryAttachment(Unpooled.copiedBuffer(attachment));
//        }

        String message = request.getMessage();
        EchoResponse response = new EchoResponse();
        response.setMessage(message);
        LOG.debug("EchoService.echo, request={}, response={}",
                request.getMessage(), response.getMessage());
        return response;
    }
}
