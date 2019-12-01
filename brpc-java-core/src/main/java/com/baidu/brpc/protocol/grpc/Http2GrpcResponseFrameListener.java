package com.baidu.brpc.protocol.grpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.*;
import lombok.Getter;

/**
 * Decode http2 response
 * @author kewei wang(kevin)
 */
public class Http2GrpcResponseFrameListener extends Http2EventAdapter {

    @Getter
    private Http2GrpcResponse http2GrpcResponse;

    public void onHeadersRead(ChannelHandlerContext ctx, final int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
        Http2HeadersFrame frame = new DefaultHttp2HeadersFrame(headers, endStream, padding).stream(new Http2FrameStream() {
            @Override
            public int id() {
                return streamId;
            }

            @Override
            public Http2Stream.State state() {
                return null;
            }
        });
        if (http2GrpcResponse == null) {
            http2GrpcResponse = new Http2GrpcResponse();
        }
        if (!endStream) {
            http2GrpcResponse.setStartHttp2Headers(frame);
        } else {
            http2GrpcResponse.setEndHttp2Headers(frame);
        }
    }

    public int onDataRead(ChannelHandlerContext ctx, final int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
        Http2DataFrame frame = new DefaultHttp2DataFrame(data, endOfStream, padding).stream(new Http2FrameStream() {
            @Override
            public int id() {
                return streamId;
            }

            @Override
            public Http2Stream.State state() {
                return null;
            }
        });
        if (http2GrpcResponse == null) {
            http2GrpcResponse = new Http2GrpcResponse();

        }
        http2GrpcResponse.setHttp2Data(frame);

        return data.readableBytes() + padding;
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, final int streamId, long errorCode) throws Http2Exception {
       /*Http2ResetFrame resetFrame = new DefaultHttp2ResetFrame(errorCode).stream(new Http2FrameStream() {
           @Override
           public int id() {
               return streamId;
           }

           @Override
           public Http2Stream.State state() {
               return null;
           }
       });*/

        // ctx.writeAndFlush(resetFrame);
        new DefaultHttp2FrameWriter().writeRstStream(ctx, streamId, errorCode, ctx.newPromise());
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
        new DefaultHttp2FrameWriter().writeSettingsAck(ctx,ctx.newPromise());

    }
}