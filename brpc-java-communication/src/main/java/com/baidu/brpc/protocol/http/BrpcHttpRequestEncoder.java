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

import static io.netty.handler.codec.http.HttpConstants.SP;

import java.util.LinkedList;
import java.util.List;

import com.baidu.brpc.exceptions.RpcException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.CharsetUtil;

/**
 * Migrate from netty {@link HttpRequestEncoder}
 *
 * @author wangjiayin@baidu.com
 * @since 2019-01-12
 */
public class BrpcHttpRequestEncoder extends BrpcHttpObjectEncoder<HttpRequest> {

    private static final char SLASH = '/';
    private static final char QUESTION_MARK = '?';
    private static final int SLASH_AND_SPACE_SHORT = (SLASH << 8) | SP;
    private static final int SPACE_SLASH_AND_SPACE_MEDIUM = (SP << 16) | SLASH_AND_SPACE_SHORT;

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return super.acceptOutboundMessage(msg) && !(msg instanceof HttpResponse);
    }

    @Override
    protected void encodeInitialLine(ByteBuf buf, HttpRequest request) throws Exception {
        ByteBufUtil.copy(request.method().asciiName(), buf);

        String uri = request.uri();

        if (uri.isEmpty()) {
            // Add " / " as absolute path if uri is not present.
            // See http://tools.ietf.org/html/rfc2616#section-5.1.2
            ByteBufUtil.writeMediumBE(buf, SPACE_SLASH_AND_SPACE_MEDIUM);
        } else {
            CharSequence uriCharSequence = uri;
            boolean needSlash = false;
            int start = uri.indexOf("://");
            if (start != -1 && uri.charAt(0) != SLASH) {
                start += 3;
                // Correctly handle query params.
                // See https://github.com/netty/netty/issues/2732
                int index = uri.indexOf(QUESTION_MARK, start);
                if (index == -1) {
                    if (uri.lastIndexOf(SLASH) < start) {
                        needSlash = true;
                    }
                } else {
                    if (uri.lastIndexOf(SLASH, index) < start) {
                        uriCharSequence = new StringBuilder(uri).insert(index, SLASH);
                    }
                }
            }
            buf.writeByte(SP).writeCharSequence(uriCharSequence, CharsetUtil.UTF_8);
            if (needSlash) {
                // write "/ " after uri
                ByteBufUtil.writeShortBE(buf, SLASH_AND_SPACE_SHORT);
            } else {
                buf.writeByte(SP);
            }
        }

        buf.writeCharSequence(request.protocolVersion().text(), CharsetUtil.US_ASCII);
        ByteBufUtil.writeShortBE(buf, CRLF_SHORT);
    }

    public ByteBuf encode(Object msg) throws Exception {
        List list = new LinkedList();
        super.encode(msg, list);
        if (list.size() == 1) {
            return (ByteBuf) list.get(0);
        } else {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "encode request failed");
        }
    }

}
