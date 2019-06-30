package com.baidu.brpc.protocol.http2;

import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;
import io.netty.util.collection.CharObjectMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.baidu.brpc.protocol.http2.Http2Handler.RESPONSE_BYTES;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class Http2GrpcProtocol extends AbstractProtocol {


    private static final ByteBuf HTTP_1_X_BUF = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[]{72, 84, 84, 80, 47, 49, 46})).asReadOnly();

    private boolean autoAckSettings = false;

    Http2Connection connection = null;
    Http2ConnectionEncoder encoder = null;
    Http2ConnectionDecoder decoder = null;


    @Override
    public Object decode(final ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest) throws BadSchemaException, TooBigDataException, NotEnoughDataException {


        //String channelId = ctx.channel().id().asLongText();

        //Http2ConnectionHolder connectionHolder = connectionMap.get(channelId);

        try {
            final List<Object> dataFrameList = new ArrayList<Object>();

        /*if(connectionHolder == null || !connectionHolder.isConnected()){
            //第一次连接

            Http2Connection connection = new DefaultHttp2Connection(true);
            Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, new DefaultHttp2FrameWriter());
            Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, new DefaultHttp2FrameReader());
            connectionHolder = new Http2ConnectionHolder(true,connection,encoder,decoder);
            connectionMap.put(channelId,connectionHolder);
        } else {

        }*/


      /*  ByteBuf byteBuf = in.readRetainedSlice(in.readableBytes());

        ByteBuf newByteBuf = byteBuf.copy();

        byte[] readableBytes = new byte[byteBuf.readableBytes()];

        byteBuf.readBytes(readableBytes);

        byte[] magicBytes = new byte[24];

        //byteBuf.readBytes(magicBytes);

        System.arraycopy(readableBytes, 0, magicBytes, 0, 24);

        String magicStr = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";
        String magicFromBytes = new String(magicBytes);
*/
        /*
        if (magicFromBytes.equals(magicStr)) {
            //first time request, need to remove magic stream
            newByteBuf = newByteBuf.readBytes(magicBytes);
        }*/


            //Http2ConnectionHandler





           /* ByteBuf inBuf = null;
            try {
                inBuf = in.nettyByteBuf();
            } catch (Exception e){

            }*/


            if (
                    ctx.channel().isActive() &&
                            readClientPrefaceString(in)
                // magicFromBytes.equals(magicStr)
                //&&
                //               verifyFirstFrameIsSettings(inBuf)


            ) {
//                autoAckSettings = false;

/*
                connection = new DefaultHttp2Connection(true);
                encoder = new DefaultHttp2ConnectionEncoder(connection, new DefaultHttp2FrameWriter());
                decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, new DefaultHttp2FrameReader(), Http2PromisedRequestVerifier.ALWAYS_VERIFY, autoAckSettings);
*/


                System.out.println("Good!");
                //newByteBuf = newByteBuf.readBytes(magicBytes);
                return dataFrameList;

            } else {
                autoAckSettings = false;

                connection = new DefaultHttp2Connection(true);
                encoder = new DefaultHttp2ConnectionEncoder(connection, new DefaultHttp2FrameWriter());
                decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, new DefaultHttp2FrameReader(), Http2PromisedRequestVerifier.ALWAYS_VERIFY, autoAckSettings);
/*            }

            {
 */               decoder.frameListener(new Http2FrameListener() {
                    @Override
                    public int onDataRead(ChannelHandlerContext channelHandlerContext, int i, ByteBuf byteBuf, int i1, boolean b) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onDataRead");
                        return 0;
                    }

                    @Override
                    public void onHeadersRead(ChannelHandlerContext channelHandlerContext, int i, Http2Headers http2Headers, int i1, boolean b) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onHeadersRead");
                        dataFrameList.add(http2Headers);
                    }

                    @Override
                    public void onHeadersRead(ChannelHandlerContext channelHandlerContext, int i, Http2Headers http2Headers, int i1, short i2, boolean b, int i3, boolean b1) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onHeadersRead");
                        dataFrameList.add(http2Headers);
                    }

                    @Override
                    public void onPriorityRead(ChannelHandlerContext channelHandlerContext, int i, int i1, short i2, boolean b) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onPriorityRead");
                    }

                    @Override
                    public void onRstStreamRead(ChannelHandlerContext channelHandlerContext, int i, long l) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onRstStreamRead");
                    }

                    @Override
                    public void onSettingsAckRead(ChannelHandlerContext channelHandlerContext) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onSettingsAckRead");
                    }

                    @Override
                    public void onSettingsRead(ChannelHandlerContext channelHandlerContext, Http2Settings http2Settings) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onSettingsRead");
                        dataFrameList.add(http2Settings);
                        // new DefaultHttp2FrameWriter().writeSettings(ctx, new Http2Settings(), ctx.newPromise());

                    }

                    @Override
                    public void onPingRead(ChannelHandlerContext channelHandlerContext, long l) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onPingRead");
                    }

                    @Override
                    public void onPingAckRead(ChannelHandlerContext channelHandlerContext, long l) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onPingAckRead");
                    }

                    @Override
                    public void onPushPromiseRead(ChannelHandlerContext channelHandlerContext, int i, int i1, Http2Headers http2Headers, int i2) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onPushPromiseRead");
                    }

                    @Override
                    public void onGoAwayRead(ChannelHandlerContext channelHandlerContext, int i, long l, ByteBuf byteBuf) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onGoAwayRead");
                    }

                    @Override
                    public void onWindowUpdateRead(ChannelHandlerContext channelHandlerContext, int i, int i1) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onWindowUpdateRead");
                        dataFrameList.add(new DefaultHttp2WindowUpdateFrame(i1));
                        //new DefaultHttp2FrameWriter().writeWindowUpdate(ctx,i,i1,ctx.newPromise());

                    }

                    @Override
                    public void onUnknownFrame(ChannelHandlerContext channelHandlerContext, byte b, int i, Http2Flags http2Flags, ByteBuf byteBuf) throws Http2Exception {
                        System.out.println("Http2GrpcProtocol.onUnknownFrame");
                    }
                });

                ByteBuf readyToDecode = in.readRetainedSlice(in.readableBytes());
                decoder.decodeFrame(ctx, readyToDecode, null);

                in.addBuffer(readyToDecode);

                System.out.println(dataFrameList);
                return dataFrameList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadSchemaException();
        }

        // return dataFrameList;
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        return null;
    }

    @Override
    public Response decodeResponse(Object msg, ChannelHandlerContext ctx) throws Exception {
        return null;
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {

        System.out.println(packet);
        Request request = new Http2GrpcRequest();
        request.setMsg(packet);
        request.setException(new RpcException());

        return request;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        Object packet = request.getMsg();

        /*List frameList = (List)packet;
        //Http2FrameWriter writer =
        for(Object frame : frameList) {
            if(frame instanceof Http2Settings){
                ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.directBuffer(9);//Unpooled.copiedBuffer(byteArr);
                //request.getChannel().alloc().buffer(9);
                writeFrameHeaderInternal(buf, 0, (byte)4, (new Http2Flags()).ack(true), 0);
                return buf;
            }
        }*/

        //new DefaultHttp2FrameWriter().writeSettingsAck();

        //return null;

        //ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.directBuffer(9);//Unpooled.copiedBuffer(byteArr);
        //request.getChannel().alloc().buffer(9);
        //writeFrameHeaderInternal(buf, 0, (byte) 4, (new Http2Flags()).ack(false), 0);
        //return buf;
        //Http2CodecUtil.writeFrameHeaderInternal(buf, 0, (byte)4, (new Http2Flags()).ack(true), 0);
        return writeSettings(new Http2Settings());
    }

    private void writeFrameHeaderInternal(ByteBuf out, int payloadLength, byte type, Http2Flags flags, int streamId) {
        out.writeMedium(payloadLength);
        out.writeByte(type);
        out.writeByte(flags.value());
        out.writeInt(streamId);
    }


    public ByteBuf writeSettings(Http2Settings settings) {
        int payloadLength = 6 * settings.size();
        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(9 + settings.size() * 6);
        writeFrameHeaderInternal(buf, payloadLength, (byte) 4, new Http2Flags(), 0);
        Iterator iter = settings.entries().iterator();

        while (iter.hasNext()) {
            CharObjectMap.PrimitiveEntry<Long> entry = (CharObjectMap.PrimitiveEntry) iter.next();
            buf.writeChar(entry.key());
            buf.writeInt(((Long) entry.value()).intValue());
        }

        return buf;
    }

    private boolean readClientPrefaceString(DynamicCompositeByteBuf dynamicIn) {
        ByteBuf in = dynamicIn.readRetainedSlice(dynamicIn.readableBytes());
        try {
            final String clientPrefaceStringStr = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";
            ByteBuf clientPrefaceString = Unpooled.copiedBuffer(clientPrefaceStringStr.getBytes());
            if (in == null || in.readableBytes() == 0) return false;

        /*if (this.clientPrefaceString == null) {
            return true;
        } else {*/
            int prefaceRemaining = clientPrefaceString.readableBytes();
            int bytesRead = Math.min(in.readableBytes(), prefaceRemaining);
            if (bytesRead != 0 && ByteBufUtil.equals(in, in.readerIndex(), clientPrefaceString, clientPrefaceString.readerIndex(), bytesRead)) {
                in.skipBytes(bytesRead);
                clientPrefaceString.skipBytes(bytesRead);
                if (!clientPrefaceString.isReadable()) {
                    clientPrefaceString.release();
                    //clientPrefaceString = null;
                    return true;
                } else {
                    return false;
                }
            } else {
                /*int maxSearch = 1024;
                int http1Index = ByteBufUtil.indexOf(HTTP_1_X_BUF, in.slice(in.readerIndex(), Math.min(in.readableBytes(), maxSearch)));
                String receivedBytes;
                if (http1Index != -1) {
                    receivedBytes = in.toString(in.readerIndex(), http1Index - in.readerIndex(), CharsetUtil.US_ASCII);
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Unexpected HTTP/1.x request: %s", new Object[]{receivedBytes});
                } else {
                    receivedBytes = ByteBufUtil.hexDump(in, in.readerIndex(), Math.min(in.readableBytes(), this.clientPrefaceString.readableBytes()));
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP/2 client preface string missing or corrupt. Hex dump for received bytes: %s", new Object[]{receivedBytes});
                }*/
                return false;
            }
        } finally {
            dynamicIn.addBuffer(in);
        }
        // }
    }

    private boolean verifyFirstFrameIsSettings(ByteBuf in) throws Http2Exception {
        if (in.readableBytes() < 5) {
            return false;
        } else {
            short frameType = in.getUnsignedByte(in.readerIndex() + 3);
            short flags = in.getUnsignedByte(in.readerIndex() + 4);
            if (frameType == 4 && (flags & 1) == 0) {
                return true;
            } else {
                throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "First received frame was not SETTINGS. Hex dump for first 5 bytes: %s", new Object[]{ByteBufUtil.hexDump(in, in.readerIndex(), 5)});
            }
        }
    }


    /**
     * If receive a frame with end-of-stream set, send a pre-canned response.
     */
    private static void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) throws Exception {
        Http2FrameStream stream = data.stream();

        if (data.isEndStream()) {
            sendResponse(ctx, stream, data.content());
        } else {
            // We do not send back the response to the remote-peer, so we need to release it.
            data.release();
        }

        // Update the flowcontroller
        ctx.write(new DefaultHttp2WindowUpdateFrame(data.initialFlowControlledBytes()).stream(stream));
    }

    /**
     * If receive a frame with end-of-stream set, send a pre-canned response.
     */
    private static void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headers)
            throws Exception {
        if (headers.isEndStream()) {
            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(RESPONSE_BYTES.duplicate());
            ByteBufUtil.writeAscii(content, " - via HTTP/2");
            sendResponse(ctx, headers.stream(), content);
        }
    }

    /**
     * Sends a "Hello World" DATA frame to the client.
     */
    private static void sendResponse(ChannelHandlerContext ctx, Http2FrameStream stream, ByteBuf payload) {
        // Send a frame for the response status
        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
        ctx.write(new DefaultHttp2HeadersFrame(headers).stream(stream));
        ctx.write(new DefaultHttp2DataFrame(payload, true).stream(stream));
    }
}
