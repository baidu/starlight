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

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.sse.ServerEvent;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.ThreadLocalChannelContext;
import com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol;
import com.baidu.cloud.starlight.protocol.http.springrest.SpringRestHttpDecoder;
import com.baidu.cloud.thirdparty.apache.commons.lang3.StringUtils;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObject;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.LastHttpContent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.baidu.cloud.starlight.api.common.Constants.SSE_EMBEDDED_CHANNEL_KEY;
import static com.baidu.cloud.starlight.api.common.Constants.SSE_REQUEST_ID_KEY;
import static com.baidu.cloud.starlight.api.common.Constants.SUCCESS_CODE;

public class SpringRestSseHttpDecoder extends SpringRestHttpDecoder {
    private static final byte[] DEFAULT_BODY = new byte[] {0};

    @Override
    public MsgBase decode(DynamicCompositeByteBuf input) throws CodecException {
        ChannelAttribute channelAttribute = ThreadLocalChannelContext.getContext().getChannelAttribute();
        ChannelSide channelSide = channelAttribute.getRpcChannel().side();

        // 服务端完整解包request
        if (channelSide == ChannelSide.SERVER) {
            return super.decode(input);
        }

        // 客户端侧在发送请求的时候在channelAttribute设置了sse协议，所以如果不是sse协议，那就跳过，避免误解其他协议的response包
        if (!StringUtils.equalsIgnoreCase(channelAttribute.getChannelProtocol(), SpringRestSseProtocol.PROTOCOL_NAME)) {
            throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "channelAttribute#getChannelProtocol not springrestsse");
        }

        // sse客户端的 response 解码进行适配
        RpcChannel rpcChannel = channelAttribute.getRpcChannel();
        // 获取跟channel绑定的 embeddedChannel
        EmbeddedChannel embeddedChannel = (EmbeddedChannel) rpcChannel.getAttribute(SSE_EMBEDDED_CHANNEL_KEY);
        if (embeddedChannel == null) {
            embeddedChannel = new EmbeddedChannel(new HttpSseResponseDecoderAdaptor());
            rpcChannel.setAttribute(SSE_EMBEDDED_CHANNEL_KEY, embeddedChannel);
        }

        ByteBuf byteBuf = input.retainedSlice(input.readableBytes());
        ByteBuf byteBufCopy = byteBuf.copy();
        boolean decodeFail = false;
        try {
            embeddedChannel.writeInbound(byteBufCopy);
            if (embeddedChannel.inboundMessages().isEmpty()) {
                // 解不出来一个包 返回空，等数据来了再试
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTENOUGHDATA_EXCEPTION,
                    "Data not enough to parse using sse");
            }

            List<HttpObject> httpObjects =
                embeddedChannel.inboundMessages().stream().map(o -> (HttpObject) o).collect(Collectors.toList());
            // 读取完之后，记得清空，不然影响下次读取
            embeddedChannel.inboundMessages().clear();

            decodeFail =
                httpObjects.stream().filter(e -> !e.getDecoderResult().isSuccess()).findAny().isPresent();
            if (decodeFail) {
                LOGGER.debug("Cannot use Http sse protocol to decode: decoded result is null or failed");
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Cannot use Http sse protocol to decode: decoded result is null");
            }

            // 如果是错误响应 ，解包出来的是一个 FullHttpResponse
            if (httpObjects.size() == 1 && httpObjects.get(0) instanceof FullHttpResponse) {
                Response response = reverseConvertResponse((FullHttpResponse) httpObjects.get(0));
                if (response != null) {
                    response.setProtocolName(SpringRestSseProtocol.PROTOCOL_NAME);
                }
                return response;
            }

            Response response;
            long corelationId = -1;
            if (httpObjects.get(0) instanceof HttpResponse) {
                HttpResponse httpResponse = (HttpResponse) httpObjects.get(0);
                if (httpResponse.headers().get(AbstractHttpProtocol.X_STARLIGHT_ID) != null) {
                    corelationId = Long.parseLong(httpResponse.headers().get(AbstractHttpProtocol.X_STARLIGHT_ID));
                    rpcChannel.setAttribute(SSE_REQUEST_ID_KEY, corelationId);
                }
                response = new RpcResponse(corelationId);
                // set attachmentKv
                response.setAttachmentKv(new HashMap<>());
                for (Map.Entry<String, String> entry : httpResponse.headers().entries()) {
                    response.getAttachmentKv().put(entry.getKey(), entry.getValue());
                }
            } else {
                corelationId = (long) rpcChannel.getAttribute(SSE_REQUEST_ID_KEY);
                response = new RpcResponse(corelationId);
            }

            response.setProtocolName(SpringRestSseProtocol.PROTOCOL_NAME);
            response.setStatus(SUCCESS_CODE);
            response.setResult(httpObjects);
            response.setBodyBytes(DEFAULT_BODY);
            return response;
        } finally {
            if (!decodeFail) {
                input.skipBytes(byteBufCopy.readerIndex());
            }
            byteBuf.release();
        }
    }

    @Override
    protected void decodeResponseBody(Response response) {
        // 处理异常情况
        if (response.getStatus() != SUCCESS_CODE) {
            super.decodeResponseBody(response);
            return;
        }

        response.setProtocolName(SpringRestSseProtocol.PROTOCOL_NAME);
        List<HttpObject> httpObjects = (List<HttpObject>) response.getResult();

        List<ServerEvent> serverEvents = new ArrayList<>();

        for (HttpObject httpObject : httpObjects) {
            if (httpObject instanceof HttpResponse) {
                serverEvents.add(ServerEvent.START_EVENT);
            } else if (httpObject instanceof LastHttpContent) {
                serverEvents.add(ServerEvent.COMPLETE_EVENT);
            } else if (httpObject instanceof HttpContent) {
                serverEvents.add(decodeServerEvent((HttpContent) httpObject));
            } else {
                throw new IllegalStateException("unsupported httpObject :" + httpObject);
            }
        }
        response.setResult(serverEvents);
    }

    private ServerEvent decodeServerEvent(HttpContent httpContent) {
        ServerEvent serverEvent = new ServerEvent();
        String content = httpContent.content().toString(StandardCharsets.UTF_8);
        String[] lines = content.split("\n");

        StringBuffer dataBuffer = new StringBuffer();
        for (String line : lines) {
            if (line.startsWith(":")) {
                serverEvent.setComment(line.replaceFirst(":", ""));
            } else if (line.startsWith("id:")) {
                serverEvent.setId(line.replaceFirst("id:", ""));
            } else if (line.startsWith("event:")) {
                serverEvent.setEvent(line.replaceFirst("event:", ""));
            } else if (line.startsWith("retry:")) {
                serverEvent.setRetry(Long.parseLong(line.replaceFirst("retry:", "").trim()));
            } else if (line.startsWith("data:")) {
                dataBuffer.append(line.replaceFirst("data:", ""));
            }
        }

        serverEvent.setData(dataBuffer.toString());
        return serverEvent;
    }
}
