package com.baidu.brpc.protocol.grpc;

import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Http2GrpcProtocol
 * @author kewei wang
 * @email kowaywang@gmail.com
 */
public class Http2GrpcProtocol extends AbstractProtocol {

    @Override
    public Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest) throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        return null;
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
        return null;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        return null;
    }
}