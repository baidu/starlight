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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.util.CharsetUtil;

/**
 * Migrate from netty {@link HttpResponseEncoder}
 *
 * @author wangjiayin@baidu.com
 * @since 2019-01-07
 */
public class BrpcHttpResponseEncoder extends BrpcHttpObjectEncoder<HttpResponse> {

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return super.acceptOutboundMessage(msg) && !(msg instanceof HttpRequest);
    }

    @Override
    protected void encodeInitialLine(ByteBuf buf, HttpResponse response) throws Exception {
        buf.writeCharSequence(response.protocolVersion().text(), CharsetUtil.US_ASCII);
        buf.writeByte(SP);
        ByteBufUtil.copy(response.status().codeAsText(), buf);
        buf.writeByte(SP);
        buf.writeCharSequence(response.status().reasonPhrase(), CharsetUtil.US_ASCII);
        ByteBufUtil.writeShortBE(buf, CRLF_SHORT);
    }

    @Override
    protected void sanitizeHeadersBeforeEncode(HttpResponse msg, boolean isAlwaysEmpty) {
        if (isAlwaysEmpty) {
            HttpResponseStatus status = msg.status();
            if (status.codeClass() == HttpStatusClass.INFORMATIONAL
                    || status.code() == HttpResponseStatus.NO_CONTENT.code()) {

                // Stripping Content-Length:
                // See https://tools.ietf.org/html/rfc7230#section-3.3.2
                msg.headers().remove(HttpHeaderNames.CONTENT_LENGTH);

                // Stripping Transfer-Encoding:
                // See https://tools.ietf.org/html/rfc7230#section-3.3.1
                msg.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
            } else if (status.code() == HttpResponseStatus.RESET_CONTENT.code()) {

                // Stripping Transfer-Encoding:
                msg.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);

                // Set Content-Length: 0
                // https://httpstatuses.com/205
                msg.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
            }
        }
    }

    @Override
    protected boolean isContentAlwaysEmpty(HttpResponse msg) {
        // Correctly handle special cases as stated in:
        // https://tools.ietf.org/html/rfc7230#section-3.3.3
        HttpResponseStatus status = msg.status();

        if (status.codeClass() == HttpStatusClass.INFORMATIONAL) {

            if (status.code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()) {
                // We need special handling for WebSockets version 00 as it will include an body.
                // Fortunally this version should not really be used in the wild very often.
                // See https://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-00#section-1.2
                return msg.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_VERSION);
            }
            return true;
        }
        return status.code() == HttpResponseStatus.NO_CONTENT.code()
                || status.code() == HttpResponseStatus.NOT_MODIFIED.code()
                || status.code() == HttpResponseStatus.RESET_CONTENT.code();
    }

    public ByteBuf encode(Object msg) throws Exception {
        List list = new LinkedList();
        super.encode(msg, list);
        if (list.size() == 1) {
            return (ByteBuf) list.get(0);
        } else {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "encode response failed");
        }
    }
}
