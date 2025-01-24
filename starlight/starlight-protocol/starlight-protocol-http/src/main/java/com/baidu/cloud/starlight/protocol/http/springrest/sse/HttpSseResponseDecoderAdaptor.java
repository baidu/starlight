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
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.channel.ChannelHandlerContext;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpDecoderConfig;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObjectAggregator;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static com.baidu.cloud.starlight.api.common.Constants.SUCCESS_CODE;

/**
 * 是为了解决以下问题而存在： sse请求的时候， server端可能返回成功响应，也可能错误响应 成功响应：是sse格式 错误响应：是普通的格式
 */
public class HttpSseResponseDecoderAdaptor extends HttpResponseDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSseResponseDecoderAdaptor.class);

    /**
     * 512m
     */
    private static final Integer MAX_CONTENT_LENGTH = 512 * 1024 * 1024;

    /**
     * 20m 以及很大了
     */
    private static final Integer MAX_CHUNK_SIZE = 20 * 1024 * 1024;

    public HttpSseResponseDecoderAdaptor() {
        // 注意这个参数设置：不允许返回不完整的chunk
        super(new HttpDecoderConfig().setAllowPartialChunks(false).setMaxChunkSize(MAX_CHUNK_SIZE));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        super.decode(ctx, buffer, out);
        // 检测到错误响应的话，直接解码出一个FullHttpResponse
        if (!out.isEmpty() && out.get(0) instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) out.get(0);
            if (response.getStatus().code() != SUCCESS_CODE) {
                ctx.pipeline().addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
            }
        }
    }

    /**
     * 为什么要重写这个方法，是因为我们不想用 ByteToMessageDecoder 里面的 CUMULATOR 实现 starlight 有个特殊的ByteBuf可以实现 CUMULATOR 效果
     * 
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            List<Object> out = new LinkedList<>();
            try {
                callDecode(ctx, (ByteBuf) msg, out);
            } catch (Exception e) {
                LOGGER.debug(
                    "Bytebuf cannot be decoded by http protocol, will retry another protocol. " + "the reason is : {}",
                    e.getMessage());
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Bytebuf cannot be decoded by http protocol, will retry another protocol. " + "The reason is "
                        + e.getMessage());
            } finally {
                for (Object result : out) {
                    ctx.fireChannelRead(result);
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
