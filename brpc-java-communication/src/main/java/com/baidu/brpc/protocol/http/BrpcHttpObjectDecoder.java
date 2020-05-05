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
package com.baidu.brpc.protocol.http;

import java.util.LinkedList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.StringUtil;

/**
 * Migrate from netty {@link HttpObjectDecoder}
 *
 * @author wangjiayin@baidu.com
 * @since 2019-01-07
 */
public class BrpcHttpObjectDecoder extends HttpObjectDecoder {

    private static final FastThreadLocal<BrpcHttpObjectDecoder> CURRENT_RPC_RESPONSE =
            new FastThreadLocal<BrpcHttpObjectDecoder>() {
                @Override
                protected BrpcHttpObjectDecoder initialValue() {
                    return new BrpcHttpObjectDecoder(true);
                }
            };

    public static BrpcHttpObjectDecoder getDecoder(boolean isDecodingRequest) {
        BrpcHttpObjectDecoder decoder = CURRENT_RPC_RESPONSE.get();
        decoder.isDecodingRequest = isDecodingRequest;
        return decoder;
    }

    private static final HttpResponseStatus UNKNOWN_STATUS = new HttpResponseStatus(999, "Unknown");

    //    private BrpcHttpObjectAggregator httpObjectAggregator = new BrpcHttpObjectAggregator(10 * 1024 * 1024);

    private boolean isDecodingRequest;

    private BrpcHttpObjectDecoder(boolean isDecodingRequest) {
        super();
        this.isDecodingRequest = isDecodingRequest;
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) throws Exception {

        return isDecodingRequest() ? new DefaultHttpRequest(
                HttpVersion.valueOf(initialLine[2]),
                HttpMethod.valueOf(initialLine[0]), initialLine[1], validateHeaders) :
                new DefaultHttpResponse(
                        HttpVersion.valueOf(initialLine[0]),
                        HttpResponseStatus.valueOf(Integer.parseInt(initialLine[1]), initialLine[2]), validateHeaders);
    }

    @Override
    protected HttpMessage createInvalidMessage() {
        return isDecodingRequest() ?
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/bad-request", validateHeaders) :
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, UNKNOWN_STATUS, validateHeaders);
    }

    @Override
    protected boolean isDecodingRequest() {
        return isDecodingRequest;
    }

    /**
     * a ByteToMessageDecoder for http message
     *
     * @return DefaultHttpRequest or DefaultHttpResponse instance if success.
     * If there's not enough bytes, method will return null
     */
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        BrpcHttpObjectAggregator httpObjectAggregator = new BrpcHttpObjectAggregator(10 * 1024 * 1024);
        this.reset();
        List httpParts = new LinkedList();
        List httpResult = new LinkedList();
        try {
            while (in.isReadable()) {
                int outSize = httpParts.size();

                if (outSize > 0) {
                    for (int i = 0; i < outSize; i++) {
                        httpObjectAggregator.aggregate(ctx, httpParts.get(i), httpResult);
                        if (httpResult.size() > 0) {
                            return httpResult.get(0);
                        }
                    }
                    // fireChannelRead(ctx, out, outSize);
                    httpParts.clear();

                    // Check if this handler was removed before continuing with decoding.
                    // If it was removed, it is not safe to continue to operate on the buffer.
                    //
                    // See:
                    // - https://github.com/netty/netty/issues/4635
                    if (ctx.isRemoved()) {
                        break;
                    }
                    outSize = 0;
                }

                int oldInputLength = in.readableBytes();
                decode(ctx, in, httpParts);
                // decodeRemovalReentryProtection(ctx, in, out);

                // Check if this handler was removed before continuing the loop.
                // If it was removed, it is not safe to continue to operate on the buffer.
                //
                // See https://github.com/netty/netty/issues/1664
                if (ctx.isRemoved()) {
                    break;
                }

                if (outSize == httpParts.size()) {
                    if (oldInputLength == in.readableBytes()) {
                        break;
                    } else {
                        continue;
                    }
                }

                if (oldInputLength == in.readableBytes()) {
                    throw new DecoderException(
                            StringUtil.simpleClassName(getClass()) +
                                    ".decode() did not read anything but decoded a message.");
                }

                if (isSingleDecode()) {
                    break;
                }
            }
            int outSize = httpParts.size();
            if (outSize > 0) {
                for (int i = 0; i < outSize; i++) {
                    httpObjectAggregator.aggregate(ctx, httpParts.get(i), httpResult);
                    if (httpResult.size() > 0) {
                        return httpResult.get(0);
                    }
                }
            }
            // decode failed, there's not enough bytes
            httpObjectAggregator.abort();
            return null;
        } catch (DecoderException e) {
            throw e;
        } catch (Exception cause) {
            throw new DecoderException(cause);
        }
    }

}
