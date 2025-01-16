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
 
package com.baidu.cloud.starlight.protocol.http.springrest.sse;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.ShuttingDownEvent;
import com.baidu.cloud.starlight.api.rpc.sse.RpcSseEmitter;
import com.baidu.cloud.starlight.api.rpc.sse.ServerEvent;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.ThreadLocalChannelContext;
import com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol;
import com.baidu.cloud.starlight.protocol.http.springrest.SpringRestHttpEncoder;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultLastHttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObject;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseEncoder;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.LastHttpContent;

import java.nio.charset.StandardCharsets;

import static com.baidu.cloud.starlight.api.common.Constants.SSE_EMBEDDED_CHANNEL_KEY;

public class SpringRestSseHttpEncoder extends SpringRestHttpEncoder {

    @Override
    protected void encodeResponseBody(Response response) {
        // 如果是异常响应，则走原本的逻辑即可
        if (response.getStatus() != Constants.SUCCESS_CODE) {
            super.encodeResponseBody(response);
        } else {
            // 如果是sse正常响应
            Object result = response.getResult();
            if (result instanceof ServerEvent) {
                if (result == ServerEvent.START_EVENT) {
                    HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
                    httpResponse.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                    httpResponse.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
                    httpResponse.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
                    // request-id
                    httpResponse.headers().set(AbstractHttpProtocol.X_STARLIGHT_ID, response.getId());
                    // 适配零信任网关SSE逻辑，参考文档：https://ku.baidu-int.com/knowledge/HFVrC7hq1Q/b_dB7xLNHi/yToZib1hj4/hZlgASbuCxSw11
                    httpResponse.headers().set("X-Accel-Buffering", "no");

                    response.setResult(httpResponse);
                } else if (result == ServerEvent.COMPLETE_EVENT) {
                    LastHttpContent lastHttpContent = new DefaultLastHttpContent();

                    response.setResult(lastHttpContent);
                } else {
                    ServerEvent serverEvent = (ServerEvent) result;
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(serverEvent.toData().getBytes(StandardCharsets.UTF_8));
                    HttpContent httpContent = new DefaultHttpContent(byteBuf);

                    response.setResult(httpContent);
                }
            } else if (result instanceof ShuttingDownEvent) {
                // TODO 暂时没有想到好的办法处理它
            }
        }
    }

    @Override
    protected ByteBuf encodeResponse(Response response) {

        if (response.getResult() == null || !(response.getResult() instanceof HttpObject)) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "RpcResponse have not been converted to HttpObject, please check");
        }

        HttpObject httpObject = (HttpObject) response.getResult();

        RpcChannel rpcChannel = ThreadLocalChannelContext.getContext().getChannelAttribute().getRpcChannel();
        // 获取跟channel绑定的 embeddedChannel
        EmbeddedChannel embeddedChannel = (EmbeddedChannel) rpcChannel.getAttribute(SSE_EMBEDDED_CHANNEL_KEY);
        if (embeddedChannel == null) {
            embeddedChannel = new EmbeddedChannel(new HttpResponseEncoder());
            rpcChannel.setAttribute(SSE_EMBEDDED_CHANNEL_KEY, embeddedChannel);
        }

        try {
            embeddedChannel.writeOutbound(httpObject);

            ByteBuf[] outputBufs = new ByteBuf[embeddedChannel.outboundMessages().size()];
            embeddedChannel.outboundMessages().toArray(outputBufs);

            // 读一次清空一次
            embeddedChannel.outboundMessages().clear();
            return Unpooled.wrappedBuffer(outputBufs);
        } catch (Exception e) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "Encode Response to ByteBuf failed: " + e.getMessage());
        }

    }
}
