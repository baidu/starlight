/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.brpc.protocol.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.*;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import static com.baidu.brpc.protocol.http2.Http2ExampleUtil.UPGRADE_RESPONSE_HEADER;
import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.logging.LogLevel.INFO;

/**
 * A simple handler that responds with the message "Hi,I'm mumubin!".
 */
public class Http2Handler extends Http2ConnectionHandler {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, Http2Handler.class);
    static final ByteBuf RESPONSE_BYTES = unreleasableBuffer(copiedBuffer("Hi,I'm mumubin!", CharsetUtil.UTF_8));

//    无参数构造函数
/*
    public Http2Handler() {
        this(new DefaultHttp2Connection(true), new Http2InboundFrameLogger(
                new DefaultHttp2FrameReader(), logger), new Http2OutboundFrameLogger(
                new DefaultHttp2FrameWriter(), logger), new SimpleHttp2FrameListener());
    }
//调用Http2ConnectionHandler的构造函数
    private Http2Handler(Http2Connection connection, Http2FrameReader frameReader, Http2FrameWriter frameWriter,
                         SimpleHttp2FrameListener listener) {
        super(connection, frameReader, frameWriter, listener);
        listener.encoder(encoder());
    }
*/

    public Http2Handler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
    }

    /**
     * Handles the cleartext HTTP upgrade event. If an upgrade occurred, sends a simple response via HTTP/2
     * on stream 1 (the stream specifically reserved for cleartext HTTP upgrade).
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
            // 应对升级请求写一个HTTP/2响应
            // Write an HTTP/2 response to the upgrade request
//            在Http2相应的头部加上是http升级至http2的
            Http2Headers headers =
                    new DefaultHttp2Headers().status(OK.codeAsText())
                    .set(new AsciiString(UPGRADE_RESPONSE_HEADER), new AsciiString("true"));
            encoder().writeHeaders(ctx, 1, headers, 0, true, ctx.newPromise());
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }

//    内部类
    private static class SimpleHttp2FrameListener extends Http2FrameAdapter {
        private Http2ConnectionEncoder encoder;

        public void encoder(Http2ConnectionEncoder encoder) {
            this.encoder = encoder;
        }

        /**
         * 读取数据
         */
        @Override
        public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
                              boolean endOfStream) throws Http2Exception {
            int processed = data.readableBytes() + padding;
            if (endOfStream) {
                sendResponse(ctx, streamId, data.retain());
            }
            return processed;
        }

        /**
         * If receive a frame with end-of-stream set, send a pre-canned response.
         */
        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                  Http2Headers headers, int streamDependency, short weight,
                                  boolean exclusive, int padding, boolean endStream) throws Http2Exception {
            if (endStream) {
                sendResponse(ctx, streamId, RESPONSE_BYTES.duplicate());
            }
        }

        /**
         * Sends a "Hi,I'm mumubin!" DATA frame to the client.
         */
        private void sendResponse(ChannelHandlerContext ctx, int streamId, ByteBuf payload) {
            // Send a frame for the response status
            Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
            encoder.writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
            encoder.writeData(ctx, streamId, payload, 0, true, ctx.newPromise());
            ctx.flush();
        }
    }
}
