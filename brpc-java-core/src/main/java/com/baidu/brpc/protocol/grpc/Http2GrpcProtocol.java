package com.baidu.brpc.protocol.grpc;

import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.compress.Compress;
import com.baidu.brpc.compress.CompressManager;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.*;
import com.baidu.brpc.protocol.grpc.client.Http2GrpcFrameWriter;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Http2GrpcProtocol
 *
 * @author kewei wang
 * @email kowaywang@gmail.com
 */
public class Http2GrpcProtocol extends AbstractProtocol {
    private static final String BRPC_SERVICE_NAME_KEY = "brpc-service-name";
    private static final String BRPC_METHOD_NAME_KEY = "brpc-method-name";


    private static final CompressManager compressManager = CompressManager.getInstance();
    private static final ServiceManager serviceManager = ServiceManager.getInstance();
    private static final Map<String, Http2ConnectionHandler> handlerMap = new ConcurrentHashMap<String, Http2ConnectionHandler>();
    private  final AtomicBoolean clientConnected = new AtomicBoolean(false);

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



        if(isDecodingRequest) {
            FrameListener frameListener = new FrameListener();
            handler.decoder().frameListener(frameListener);

            try {
                handler.decode(ctx, readyToDecode, new ArrayList());
            } catch (Exception e) {
                handleDecodeException(ctx, channelId, handler);
                throw new BadSchemaException(e);
            }
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

            Http2GrpcResponseFrameListener frameListener = new Http2GrpcResponseFrameListener();
            handler.decoder().frameListener(frameListener);

            try {
                handler.decode(ctx, readyToDecode, new ArrayList());
            } catch (Exception e) {
                handleDecodeException(ctx, channelId, handler);
                throw new BadSchemaException(e);
            }
            Http2GrpcResponse response = frameListener.getHttp2GrpcResponse();

            return response;
        }
    }

    //TODO encode request here
    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        Channel channel = request.getChannel();
        String serviceName = request.getServiceName();
        String methodName = request.getMethodName();
        Object protoObject = request.getArgs()[0];

        /*Http2Connection connection = new DefaultHttp2Connection(true);
        Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, new DefaultHttp2FrameWriter());
*/
        Http2Headers requestHeader = new DefaultHttp2Headers();
        requestHeader.method("POST");
        requestHeader.path("/"+serviceName + "/"+ methodName);
        requestHeader.add("content-type", "application/grpc+proto");

        //Http2DataFrame http2DataFrame = new DefaultHttp2DataFrame();
        int compressType = Options.CompressType.COMPRESS_TYPE_NONE_VALUE;
        Compress compress = compressManager.getCompress(compressType);
        RpcMethodInfo rpcMethodInfo = request.getRpcMethodInfo(); //serviceManager.getService(serviceName, methodName);
        ByteBuf requestData = compress.compressInput(protoObject,rpcMethodInfo);
        ByteBuf resultDataBuf = channel.alloc().buffer(2 + 4 + requestData.readableBytes());
        resultDataBuf.writeShort(0); // compress flag (0 means no compress)
        resultDataBuf.writeMedium(requestData.readableBytes()); // data length
        resultDataBuf.writeBytes(requestData); // data content

        Http2GrpcFrameWriter frameWriter = new Http2GrpcFrameWriter();
        frameWriter.writeHeaders(channel,1,requestHeader,0,false,channel.newPromise());
        frameWriter.writeData(channel,1,resultDataBuf,0,true,channel.newPromise());
        channel.flush();

        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public Response decodeResponse(Object msg, ChannelHandlerContext ctx) throws Exception {
        if(msg != null) {
            Http2GrpcResponse http2GrpcResponse = (Http2GrpcResponse)msg;
            Http2Headers http2Headers = http2GrpcResponse.getStartHttp2Headers().headers();
            RpcMethodInfo rpcMethodInfo = serviceManager.getService(
                    http2Headers.get(BRPC_SERVICE_NAME_KEY).toString(),
                    http2Headers.get(BRPC_METHOD_NAME_KEY).toString());

            ByteBuf protoAndAttachmentBuf = http2GrpcResponse.getHttp2Data().content();

            /*
            Necessary operation
             */
            byte compressFlag = protoAndAttachmentBuf.readByte();
            int messageLength = protoAndAttachmentBuf.readInt();

            int compressType = Options.CompressType.COMPRESS_TYPE_NONE_VALUE;

            Compress compress = compressManager.getCompress(compressType);
            Object proto = compress.uncompressOutput(protoAndAttachmentBuf, rpcMethodInfo);

            http2GrpcResponse.setResult(proto);

            return http2GrpcResponse;
        } else return new RpcResponse();
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
        responseHeader.add(BRPC_SERVICE_NAME_KEY,request.getServiceName());
        responseHeader.add(BRPC_METHOD_NAME_KEY,request.getMethodName());

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

        Http2GrpcFrameWriter frameWriter = new Http2GrpcFrameWriter();
        try {
            if(!clientConnected.get()) {
                Channel clientChannel = channelGroup.getChannel();
                //https://tools.ietf.org/html/rfc7540#page-11
                clientChannel.write(Http2CodecUtil.connectionPrefaceBuf());
                //https://tools.ietf.org/html/rfc7540#section-6.5
                frameWriter.writeSettings(clientChannel, new Http2Settings(), clientChannel.newPromise());
                frameWriter.writeWindowUpdate(clientChannel,0,74,clientChannel.newPromise());
                clientChannel.flush();
                clientConnected.compareAndSet(false,true);
            }

        } catch(Exception e) {}

    }
}