package com.baidu.brpc.protocol.grpc;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.compress.Compress;
import com.baidu.brpc.compress.CompressManager;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Http2GrpcProtocol
 *
 * @author kewei wang
 * @email kowaywang@gmail.com
 */
public class Http2GrpcProtocol extends AbstractProtocol {


    private static final CompressManager compressManager = CompressManager.getInstance();
    private static final ServiceManager serviceManager = ServiceManager.getInstance();
    private Map<String, Http2ConnectionHandler> handlerMap = new ConcurrentHashMap<String, Http2ConnectionHandler>();

    @Override
    public Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest) throws BadSchemaException, TooBigDataException, NotEnoughDataException {

        String channelId = ctx.channel().id().asLongText();
        Http2ConnectionHandler handler = handlerMap.get(channelId);

        if (handler == null) {
            Http2Connection connection = new DefaultHttp2Connection(true);
            Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, new DefaultHttp2FrameWriter());
            Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, new DefaultHttp2FrameReader(), Http2PromisedRequestVerifier.ALWAYS_VERIFY, true);

            handler = new Http2ConnectionHandler(true, new DefaultHttp2FrameWriter(),
                    null, new Http2Settings());
            try {
                handler.handlerAdded(ctx);
                decoder.lifecycleManager(handler);
                handlerMap.put(channelId, handler);
            } catch(Exception e){
                throw new BadSchemaException(e);
            }
        }

        ByteBuf readyToDecode = in.readRetainedSlice(in.readableBytes());

        FrameListener frameListener = new FrameListener();
        handler.decoder().frameListener(frameListener);

        try {
            handler.decode(ctx, readyToDecode, new ArrayList());
        } catch (Exception e) {
            handleDecodeException(ctx, channelId, handler);
            throw new BadSchemaException(e);
        }

        if(isDecodingRequest) {
            Http2GrpcRequest request = frameListener.getHttp2GrpcRequest();
            if(request != null) {
                Http2Headers requestHeaders = request.getHttp2Headers().headers();
                CharSequence path = requestHeaders.path();
                String pathStr = path.toString();
                String[] arr = pathStr.split("/");
                String serviceName = arr[1];
                String methodName = arr[2];

                int compressType = Options.CompressType.COMPRESS_TYPE_NONE_VALUE;
                request.setCompressType(compressType);
                request.setServiceName(serviceName);
                request.setMethodName(methodName);
                request.setChannelHandlerContext(ctx);

                return request;
            } else return null;
        } else {
            //TODO decode response here
            return null;
        }
    }

    //TODO encode request here
    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        String serviceName = request.getServiceName();
        String methodName = request.getMethodName();
        Object protoObject = request.getArgs()[0];

        Http2Connection connection = new DefaultHttp2Connection(true);
        Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, new DefaultHttp2FrameWriter());

        Http2HeadersEncoder headersEncoder = new DefaultHttp2HeadersEncoder();

        Http2Headers requestHeader = new DefaultHttp2Headers();
        requestHeader.method("POST");
        requestHeader.path("/"+serviceName + "/"+ methodName);
        requestHeader.add("content-type", "application/grpc+proto");

        ByteBuf result = request.getChannel().alloc().buffer();
        headersEncoder.encodeHeaders(0,requestHeader,result);

        return Unpooled.wrappedBuffer(result);
    }

    @Override
    public Response decodeResponse(Object msg, ChannelHandlerContext ctx) throws Exception {
        //TODO decode response body here
        return null;
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        if(packet != null) {
            Http2GrpcRequest http2GrpcRequest = (Http2GrpcRequest) packet;

            RpcMethodInfo rpcMethodInfo = serviceManager.getService(
                    http2GrpcRequest.getServiceName(), http2GrpcRequest.getMethodName());

            ByteBuf protoAndAttachmentBuf = http2GrpcRequest.getHttp2Data().content();

            /*
            Necessary operation
             */
            byte compressFlag = protoAndAttachmentBuf.readByte();
            int messageLength = protoAndAttachmentBuf.readInt();

            Compress compress = compressManager.getCompress(http2GrpcRequest.getCompressType());
            Object proto = compress.uncompressInput(protoAndAttachmentBuf, rpcMethodInfo);
            http2GrpcRequest.setArgs(new Object[]{proto});
            http2GrpcRequest.setRpcMethodInfo(rpcMethodInfo);
            http2GrpcRequest.setTargetMethod(rpcMethodInfo.getMethod());
            http2GrpcRequest.setTarget(rpcMethodInfo.getTarget());

            return http2GrpcRequest;
        } else return null;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {

        if (request == null) return Unpooled.EMPTY_BUFFER;

        Http2GrpcRequest http2GrpcRequest = (Http2GrpcRequest) request;
        Object responseProto = response.getResult();


        int streamId = http2GrpcRequest.getHttp2Headers().stream().id();
        String channelId = request.getChannel().id().asLongText();

        Http2ConnectionHandler handler = handlerMap.get(channelId);
        ChannelHandlerContext ctx = http2GrpcRequest.getChannelHandlerContext();

        Http2Headers responseHeader = new DefaultHttp2Headers();
        responseHeader.status("200");
        responseHeader.add("content-type", "application/grpc+proto");

        Http2Headers responseEndHeader = new DefaultHttp2Headers();
        responseEndHeader.add("grpc-status", "0");

        if (responseProto != null) {
            Compress compress = compressManager.getCompress(request.getCompressType());
            ByteBuf protoBuf = compress.compressOutput(responseProto, response.getRpcMethodInfo());
            ByteBuf resultDataBuf = ctx.alloc().buffer(2 + 4 + protoBuf.readableBytes());

            resultDataBuf.writeShort(0); // compress flag (0 means no compress)
            resultDataBuf.writeMedium(protoBuf.readableBytes()); // data length
            resultDataBuf.writeBytes(protoBuf); // data content


            ChannelPromise promise = ctx.newPromise();
            Http2FrameWriter frameWriter = handler.encoder().frameWriter();
            frameWriter.writeHeaders(ctx, streamId, responseHeader, 0, false, promise);
            frameWriter.writeData(ctx, streamId, resultDataBuf, 0, false, promise);
            frameWriter.writeHeaders(ctx, streamId, responseEndHeader, 0, true, promise);

        }


        return Unpooled.EMPTY_BUFFER;
    }

    private void handleDecodeException(ChannelHandlerContext ctx, String channelId, Http2ConnectionHandler handler) {
        try {
            if (handler != null) {
                handler.close(ctx, ctx.newPromise());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            handlerMap.remove(channelId);
        }
    }

    @Override
    public void beforeRequestSent(Request request, RpcClient rpcClient, BrpcChannel channelGroup) {
        // if the http2 connection does not established, we should send connect preface to server

    }
}