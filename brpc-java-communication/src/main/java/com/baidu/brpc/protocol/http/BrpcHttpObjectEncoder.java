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

import static io.netty.buffer.Unpooled.directBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectEncoder;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

/**
 * Migrate from netty {@link HttpObjectEncoder}
 *
 * @author wangjiayin@baidu.com
 * @since 2019-01-12
 */
public abstract class BrpcHttpObjectEncoder<H extends HttpMessage> {
    static final int CRLF_SHORT = (CR << 8) | LF;
    private static final int ZERO_CRLF_MEDIUM = ('0' << 16) | CRLF_SHORT;
    private static final byte[] ZERO_CRLF_CRLF = {'0', CR, LF, CR, LF};
    private static final ByteBuf CRLF_BUF = unreleasableBuffer(directBuffer(2).writeByte(CR).writeByte(LF));
    private static final ByteBuf ZERO_CRLF_CRLF_BUF = unreleasableBuffer(directBuffer(ZERO_CRLF_CRLF.length)
            .writeBytes(ZERO_CRLF_CRLF));
    private static final float HEADERS_WEIGHT_NEW = 1 / 5f;
    private static final float HEADERS_WEIGHT_HISTORICAL = 1 - HEADERS_WEIGHT_NEW;
    private static final float TRAILERS_WEIGHT_NEW = HEADERS_WEIGHT_NEW;
    private static final float TRAILERS_WEIGHT_HISTORICAL = HEADERS_WEIGHT_HISTORICAL;

    private static final int ST_INIT = 0;
    private static final int ST_CONTENT_NON_CHUNK = 1;
    private static final int ST_CONTENT_CHUNK = 2;
    private static final int ST_CONTENT_ALWAYS_EMPTY = 3;

    @SuppressWarnings("RedundantFieldInitialization")
    private int state = ST_INIT;

    /**
     * Used to calculate an exponential moving average of the encoded size of the initial line and the headers for
     * a guess for future buffer allocations.
     */
    private float headersEncodedSizeAccumulator = 256;

    /**
     * Used to calculate an exponential moving average of the encoded size of the trailers for
     * a guess for future buffer allocations.
     */
    private float trailersEncodedSizeAccumulator = 256;

    protected void encode(Object msg, List<Object> out) throws Exception {
        ByteBuf buf = null;
        if (msg instanceof HttpMessage) {
            if (state != ST_INIT) {
                throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg)
                        + ", state: " + state);
            }

            @SuppressWarnings({"unchecked", "CastConflictsWithInstanceof"})
            H m = (H) msg;

            buf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, (int) headersEncodedSizeAccumulator);
            //            buf = ctx.alloc().buffer((int) headersEncodedSizeAccumulator);
            // Encode the message.
            encodeInitialLine(buf, m);
            state = isContentAlwaysEmpty(m) ? ST_CONTENT_ALWAYS_EMPTY :
                    HttpUtil.isTransferEncodingChunked(m) ? ST_CONTENT_CHUNK : ST_CONTENT_NON_CHUNK;

            sanitizeHeadersBeforeEncode(m, state == ST_CONTENT_ALWAYS_EMPTY);

            encodeHeaders(m.headers(), buf);
            ByteBufUtil.writeShortBE(buf, CRLF_SHORT);

            headersEncodedSizeAccumulator = HEADERS_WEIGHT_NEW * padSizeForAccumulation(buf.readableBytes()) +
                    HEADERS_WEIGHT_HISTORICAL * headersEncodedSizeAccumulator;
        }

        // Bypass the encoder in case of an empty buffer, so that the following idiom works:
        //
        //     ch.write(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        //
        // See https://github.com/netty/netty/issues/2983 for more information.
        //        if (msg instanceof ByteBuf) {
        //            final ByteBuf potentialEmptyBuf = (ByteBuf) msg;
        //            if (!potentialEmptyBuf.isReadable()) {
        //                out.add(potentialEmptyBuf.retain());
        //                return;
        //            }
        //        }

        if (msg instanceof HttpContent || msg instanceof ByteBuf || msg instanceof FileRegion) {
            switch (state) {
                case ST_INIT:
                    throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
                case ST_CONTENT_NON_CHUNK:
                    final long contentLength = contentLength(msg);
                    if (contentLength > 0 && msg instanceof HttpContent) {
                        if (buf != null) {
                            buf.ensureWritable((int) contentLength);
                            buf.writeBytes(((HttpContent) msg).content());
                            out.add(buf);
                        }

                        //                        if (buf != null && buf.writableBytes() >= contentLength && msg instanceof HttpContent) {
                        //                            // merge into other buffer for performance reasons
                        //                            buf.writeBytes(((HttpContent) msg).content());
                        //                            out.add(buf);
                        //                        } else {
                        //                            if (buf != null) {
                        //                                out.add(buf);
                        //                            }
                        //                            out.add(encodeAndRetain(msg));
                        //                        }
                        //
                        //                        if (msg instanceof LastHttpContent) {
                        //                            state = ST_INIT;
                        //                        }

                        break;
                    }

                    // fall-through!
                case ST_CONTENT_ALWAYS_EMPTY:

                    if (buf != null) {
                        // We allocated a buffer so add it now.
                        out.add(buf);
                    } else {
                        // Need to produce some output otherwise an
                        // IllegalStateException will be thrown as we did not write anything
                        // Its ok to just write an EMPTY_BUFFER as if there are reference count issues these will be
                        // propagated as the caller of the encode(...) method will release the original
                        // buffer.
                        // Writing an empty buffer will not actually write anything on the wire, so if there is a user
                        // error with msg it will not be visible externally
                        out.add(Unpooled.EMPTY_BUFFER);
                    }

                    break;
                case ST_CONTENT_CHUNK:
                    if (buf != null) {
                        // We allocated a buffer so add it now.
                        out.add(buf);
                    }
                    encodeChunkedContent(msg, contentLength(msg), out);

                    break;
                default:
                    throw new Error();
            }

            if (msg instanceof LastHttpContent) {
                state = ST_INIT;
            }
        } else if (buf != null) {
            out.add(buf);
        }
    }

    /**
     * Encode the {@link HttpHeaders} into a {@link ByteBuf}.
     */
    protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
        Iterator<Map.Entry<CharSequence, CharSequence>> iter = headers.iteratorCharSequence();
        while (iter.hasNext()) {
            Map.Entry<CharSequence, CharSequence> header = iter.next();
            HttpHeadersEncoder.encoderHeader(header.getKey(), header.getValue(), buf);
        }
    }

    private void encodeChunkedContent(Object msg, long contentLength, List<Object> out) {
        if (contentLength > 0) {
            String lengthHex = Long.toHexString(contentLength);
            ByteBuf buf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, lengthHex.length() + 2);
            buf.writeCharSequence(lengthHex, CharsetUtil.US_ASCII);
            ByteBufUtil.writeShortBE(buf, CRLF_SHORT);
            out.add(buf);
            out.add(encodeAndRetain(msg));
            out.add(CRLF_BUF.duplicate());
        }

        if (msg instanceof LastHttpContent) {
            HttpHeaders headers = ((LastHttpContent) msg).trailingHeaders();
            if (headers.isEmpty()) {
                out.add(ZERO_CRLF_CRLF_BUF.duplicate());
            } else {
                ByteBuf buf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false,
                        (int) trailersEncodedSizeAccumulator);
                ByteBufUtil.writeMediumBE(buf, ZERO_CRLF_MEDIUM);
                encodeHeaders(headers, buf);
                ByteBufUtil.writeShortBE(buf, CRLF_SHORT);
                trailersEncodedSizeAccumulator = TRAILERS_WEIGHT_NEW * padSizeForAccumulation(buf.readableBytes()) +
                        TRAILERS_WEIGHT_HISTORICAL * trailersEncodedSizeAccumulator;
                out.add(buf);
            }
        } else if (contentLength == 0) {
            // Need to produce some output otherwise an
            // IllegalStateException will be thrown
            out.add(encodeAndRetain(msg));
        }
    }

    /**
     * Allows to sanitize headers of the message before encoding these.
     */
    protected void sanitizeHeadersBeforeEncode(@SuppressWarnings("unused") H msg, boolean isAlwaysEmpty) {
        // noop
    }

    /**
     * Determine whether a message has a content or not. Some message may have headers indicating
     * a content without having an actual content, e.g the response to an HEAD or CONNECT request.
     *
     * @param msg the message to test
     *
     * @return {@code true} to signal the message has no content
     */
    protected boolean isContentAlwaysEmpty(@SuppressWarnings("unused") H msg) {
        return false;
    }

    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return msg instanceof HttpObject || msg instanceof ByteBuf || msg instanceof FileRegion;
    }

    private static Object encodeAndRetain(Object msg) {
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).retain();
        }
        if (msg instanceof HttpContent) {
            return ((HttpContent) msg).content().retain();
        }
        if (msg instanceof FileRegion) {
            return ((FileRegion) msg).retain();
        }
        throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
    }

    private static long contentLength(Object msg) {
        if (msg instanceof HttpContent) {
            return ((HttpContent) msg).content().readableBytes();
        }
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        }
        if (msg instanceof FileRegion) {
            return ((FileRegion) msg).count();
        }
        throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
    }

    /**
     * Add some additional overhead to the buffer. The rational is that it is better to slightly over allocate and waste
     * some memory, rather than under allocate and require a resize/copy.
     *
     * @param readableBytes The readable bytes in the buffer.
     *
     * @return The {@code readableBytes} with some additional padding.
     */
    private static int padSizeForAccumulation(int readableBytes) {
        return (readableBytes << 2) / 3;
    }

    @Deprecated
    protected static void encodeAscii(String s, ByteBuf buf) {
        buf.writeCharSequence(s, CharsetUtil.US_ASCII);
    }

    protected abstract void encodeInitialLine(ByteBuf buf, H message) throws Exception;

}
