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
 
package com.baidu.cloud.starlight.protocol.http;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;

import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Combine {@link io.netty.handler.codec.http.HttpRequestDecoder} and
 * {@link io.netty.handler.codec.http.HttpResponseDecoder} to HttpRequestResponseDecoder.
 * <p>
 * Un Thread Safe. Created by liuruisen on 2020/6/1.
 */
public class HttpRequestResponseDecoder extends HttpObjectDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestResponseDecoder.class);

    private static final String HTTP = "HTTP";

    private static final HttpResponseStatus UNKNOWN_STATUS = new HttpResponseStatus(999, "Unknown");

    private Boolean isRequest = false;

    /**
     * 512m
     */
    private static final Integer MAX_CONTENT_LENGTH = 512 * 1024 * 1024;

    private static final Integer MAX_HEADER_SIZE = 8192;

    private static final Integer MAX_INIT_LINE_LENGTH = 4096;

    public HttpRequestResponseDecoder() {
        super(MAX_INIT_LINE_LENGTH, MAX_HEADER_SIZE, MAX_CONTENT_LENGTH, true);
    }

    @Override
    protected boolean isDecodingRequest() {
        return isRequest;
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) throws Exception {
        if (initialLine[0].contains(HTTP)) { // response
            isRequest = false;
            return new DefaultHttpResponse(HttpVersion.valueOf(initialLine[0]),
                HttpResponseStatus.valueOf(Integer.parseInt(initialLine[1]), initialLine[2]), validateHeaders);
        } else { // request
            isRequest = true;
            return new DefaultHttpRequest(HttpVersion.valueOf(initialLine[2]), HttpMethod.valueOf(initialLine[0]),
                initialLine[1], validateHeaders);
        }
    }

    @Override
    protected HttpMessage createInvalidMessage() {
        if (isRequest) {
            return new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/bad-request", validateHeaders);
        } else {
            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, UNKNOWN_STATUS, validateHeaders);
        }
    }

    /**
     * 包含针对粘包拆包的处理
     * 
     * @param ctx
     * @param buffer
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {

        // Store http message part for aggregation in HttpObjectAggregator
        List<Object> httpMessageParts = new LinkedList<>();

        // 记录运行过程中所有的parts
        List<Object> allHttpMessageParts = new LinkedList<>();

        // Store full http message to return
        List<Object> fullHttpMessages = new LinkedList<>();

        HttpRequestResponseAggregator aggregator = new HttpRequestResponseAggregator(MAX_CONTENT_LENGTH);

        try {
            while (buffer.isReadable()) {
                // <1> decode byteBuf to httpMessage
                int oldPartSize = httpMessageParts.size();
                int oldBufferLength = buffer.readableBytes();

                super.decode(ctx, buffer, httpMessageParts); // produce one more http message part

                // decode one time without producing output, such as READ_CHUNK_DELIMITER BAD_MESSAGE UPGRADED
                if (oldPartSize == httpMessageParts.size() && oldBufferLength == buffer.readableBytes()) {
                    // 有可能是协议不匹配
                    LOGGER.warn("Decode one time without reading any bytes, this is unlikely to happen");
                    break;
                }

                // 第一次decode出请求头，等待后续decode出body
                if (httpMessageParts.size() == 1 && httpMessageParts.get(0) instanceof HttpMessage
                    && buffer.isReadable()) {
                    HttpMessage requestHeader = (HttpMessage) httpMessageParts.get(0);
                    String contentLengthStr = requestHeader.headers().get("Content-Length");
                    if (StringUtils.isNumeric(contentLengthStr)) {
                        int length = Integer.parseInt(contentLengthStr);
                        if (length > MAX_CONTENT_LENGTH) {
                            LOGGER
                                .error("Content-Length of request {} is {}, bigger than the max {}",
                                    requestHeader instanceof DefaultHttpRequest
                                        ? ((DefaultHttpRequest) requestHeader).uri() : requestHeader,
                                    length, MAX_CONTENT_LENGTH);
                        }
                        LOGGER.debug("Content-Length of request {} is {}, readable bytebuf is {}",
                            requestHeader instanceof DefaultHttpRequest ? ((DefaultHttpRequest) requestHeader).uri()
                                : requestHeader,
                            length, buffer.readableBytes());
                        buffer = buffer.readSlice(length);
                    }
                }
                allHttpMessageParts.addAll(httpMessageParts);

                // <2> try to aggregate http message into full http message
                for (Object httpMessagePart : httpMessageParts) {
                    aggregator.aggregate(ctx, httpMessagePart, fullHttpMessages);
                    if (fullHttpMessages.size() > 0) {
                        out.add(fullHttpMessages.get(0));
                        return;
                    }
                }
                // clear parts list, because we had aggregated parts before
                httpMessageParts.clear();
            }
        } finally {
            LOGGER.debug("allHttpMessageParts size {}", allHttpMessageParts.size());
            LOGGER.debug("fullHttpMessages size {}", fullHttpMessages.size());
            // httpMessageParts不为空，但fullHttpMessage为空，证明本次解析失败了
            // 释放httpMessageParts防止内存泄露
            if (allHttpMessageParts.size() > 0 && fullHttpMessages.isEmpty()) {
                // release拆包时解析了一半的body content
                LOGGER.info("allHttpMessageParts size {}, fullHttpMessage size {}, will release",
                    allHttpMessageParts.size(), fullHttpMessages.size());
                allHttpMessageParts.forEach(part -> {
                    if (part instanceof ReferenceCounted) {
                        ReferenceCounted refer = (ReferenceCounted) part;
                        int refCnt = refer.refCnt();
                        if (refCnt > 0) {
                            refer.release(refCnt);
                        }
                    }
                });
            }

            if (fullHttpMessages.isEmpty()) {
                // release aggregator's currentMessage(directByteBuf)
                LOGGER.debug("release aggregator's currentMessage(directByteBuf)");
                aggregator.handlerRemoved(ctx);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            List<Object> out = new LinkedList<>();
            try {
                decode(ctx, (ByteBuf) msg, out);
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
