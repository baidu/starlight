package com.baidu.brpc.protocol.grpc;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
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
import io.netty.channel.ChannelHandlerContext;
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
            } catch (Exception e) {
                throw new BadSchemaException(e);
            }
        }

        ByteBuf readyToDecode = in.readRetainedSlice(in.readableBytes());

        FrameListener frameListener = new FrameListener();
        handler.decoder().frameListener(frameListener);

        try {
            handler.decode(ctx, readyToDecode, new ArrayList());
        } catch (Exception e) {
            throw new BadSchemaException(e);
        }

        Http2GrpcRequest request = frameListener.getHttp2GrpcRequest();

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

        return request;
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

        Http2GrpcRequest http2GrpcRequest = (Http2GrpcRequest)packet;

        RpcMethodInfo rpcMethodInfo = serviceManager.getService(
                http2GrpcRequest.getServiceName(), http2GrpcRequest.getMethodName());

        ByteBuf protoAndAttachmentBuf = http2GrpcRequest.getHttp2Data().content();

        Compress compress = compressManager.getCompress(http2GrpcRequest.getCompressType());
        Object proto = compress.uncompressInput(protoAndAttachmentBuf, rpcMethodInfo);
        http2GrpcRequest.setArgs(new Object[]{proto});
        http2GrpcRequest.setRpcMethodInfo(rpcMethodInfo);
        http2GrpcRequest.setTargetMethod(rpcMethodInfo.getMethod());
        http2GrpcRequest.setTarget(rpcMethodInfo.getTarget());


        return http2GrpcRequest;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        return null;
    }
}