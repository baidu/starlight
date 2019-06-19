package com.baidu.brpc.protocol.push.impl;

import static com.baidu.brpc.protocol.push.impl.DefaultSPHead.PROVIDER_LENGTH;
import static com.baidu.brpc.protocol.push.impl.DefaultSPHead.SPHEAD_LENGTH;
import static com.baidu.brpc.protocol.push.impl.DefaultSPHead.SPHEAD_MAGIC_NUM;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.protocol.push.SPHead;
import com.baidu.brpc.protocol.push.ServerPushProtocol;
import com.baidu.brpc.server.PushServerRpcFutureManager;
import com.baidu.brpc.server.ServiceManager;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unchecked")
public class DefaultServerPushProtocol implements ServerPushProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultServerPushProtocol.class);
    protected String encoding = "utf-8";

    public DefaultServerPushProtocol(String encoding) {
        if (encoding != null) {
            this.encoding = encoding;
        }
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        Validate.notEmpty(request.getArgs(), "args must not be empty");
        byte[] bodyBytes = encodeRequestBody(request, request.getRpcMethodInfo());
        DefaultSPHead spHead = (DefaultSPHead) request.getSpHead();
        Validate.notNull(bodyBytes);
        spHead.bodyLength = bodyBytes.length;
        spHead.logId = request.getLogId();
        byte[] nsHeadBytes = headToBytes(spHead);
        return Unpooled.wrappedBuffer(nsHeadBytes, bodyBytes);
    }

    @Override
    public void beforeRequestSent(Request request, RpcClient rpcClient, BrpcChannel channelGroup) {

    }

    /**
     * decode 从客户端返回的 serverPushResponse
     *
     * @param in
     * @param ctx
     *
     * @return
     *
     * @throws Exception
     */
    public Response decodeServerPushResponse(Object in, ChannelHandlerContext ctx) {
        DefaultServerPushPacket packet = (DefaultServerPushPacket) in;
        RpcResponse rpcResponse = new RpcResponse();
        // channel info是在客户端生成连接池的时候生成的
        // ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        Long logId = (long) packet.getSpHead().getLogId();

        RpcFuture future = PushServerRpcFutureManager.getInstance().removeRpcFuture(logId);
        rpcResponse.setLogId(logId);

        if (future == null) {
            return rpcResponse;
        }
        rpcResponse.setRpcFuture(future);

        ByteBuf bodyBuf = packet.getBodyBuf();
        SPBody spBody = decodeBodyByteBuf(bodyBuf);
        Object responseBody = spBody.getContent();
        if (responseBody == null) {
            return null;
        }

        rpcResponse.setResult(responseBody);
        return rpcResponse;
    }

    @Override
    public Response decodeResponse(Object in, ChannelHandlerContext ctx) throws Exception {
        DefaultServerPushPacket packet = (DefaultServerPushPacket) in;
        DefaultSPHead spHead = (DefaultSPHead) packet.getSpHead();
        RpcResponse rpcResponse = new RpcResponse();
        // channel info是在客户端生成连接池的时候生成的
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        Long logId = (long) packet.getSpHead().getLogId();
        rpcResponse.setLogId(logId);
        RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());

        if (future == null) {
            return rpcResponse;
        }
        rpcResponse.setRpcFuture(future);
        SPBody spBody = decodeBodyByteBuf(packet.getBodyBuf());
        Object responseBody = spBody.getContent();
        if (responseBody == null) {
            return null;
        }

        rpcResponse.setResult(responseBody);
        return rpcResponse;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        byte[] bodyBytes = encodeResponseBody(response.getResult(), response.getRpcMethodInfo());
        DefaultSPHead spHead = (DefaultSPHead) response.getSpHead();
        if (spHead == null) {
            spHead = new DefaultSPHead((int) response.getLogId(), bodyBytes.length);
        } else {
            spHead.bodyLength = bodyBytes.length;
        }
        spHead.logId = response.getLogId();
        spHead.type = SPHead.TYPE_RESPONSE;
        byte[] headBytes = headToBytes(spHead);
        return Unpooled.wrappedBuffer(headBytes, bodyBytes);
    }

    @Override
    public void afterResponseSent(Request request, Response response, ChannelFuture channelFuture) {

    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = this.createRequest();
        DefaultServerPushPacket spPacket = (DefaultServerPushPacket) packet;
        ByteBuf bodyBuf = ((DefaultServerPushPacket) packet).getBodyBuf();
        request.setLogId(spPacket.getSpHead().getLogId());
        request.setSpHead(((DefaultServerPushPacket) packet).getSpHead());
        SPBody spBody = decodeBodyByteBuf(bodyBuf);
        decodeRequestBody(spBody, request);
        //request.setTarget(rpcMethodInfo.getTarget());
        request.setArgs(spBody.getParameters());
        request.setMethodName(spBody.getMethodName());
        request.setServiceName(spBody.getServiceName());
        //  request.setTargetMethod(rpcMethodInfo.getMethod());
        return request;
    }

    @Override
    public boolean returnChannelBeforeResponse() {
        return false;
    }

    @Override
    public DefaultServerPushPacket decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in,
                                          boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < DefaultSPHead.SPHEAD_LENGTH) {
            throw new NotEnoughDataException();
        }
        DefaultServerPushPacket packet = new DefaultServerPushPacket();
        ByteBuf fixHeaderBuf = in.retainedSlice(DefaultSPHead.SPHEAD_LENGTH);
        try {

            DefaultSPHead spHead = headFromByteBuf(fixHeaderBuf);
            packet.setSpHead(spHead);
            int bodyLength = spHead.bodyLength;

            // 512M
            if (bodyLength > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodyLength);
            }

            if (in.readableBytes() < DefaultSPHead.SPHEAD_LENGTH + bodyLength) {
                throw new NotEnoughDataException();
            }

            in.skipBytes(DefaultSPHead.SPHEAD_LENGTH);
            ByteBuf bodyBuf = in.readRetainedSlice(bodyLength);
            packet.setBodyBuf(bodyBuf);
            return packet;
        } catch (Exception e) {
            LOG.error("error:", e);
            throw new RuntimeException("decode failed:" + e.getMessage());
        } finally {
            fixHeaderBuf.release();
        }
    }

    public SPBody decodeBodyByteBuf(ByteBuf bodyByteBuf) {
        try {
            int readableBytes = bodyByteBuf.readableBytes();
            byte[] bodyBytes = new byte[readableBytes];
            bodyByteBuf.readBytes(bodyBytes);
            Schema<SPBody> schema = RuntimeSchema.getSchema(SPBody.class);
            SPBody spBody = new SPBody();
            ProtobufIOUtil.mergeFrom(bodyBytes, spBody, schema);
            return spBody;
        } catch (Exception e) {
            throw new RpcException(e);
        } finally {
            if (bodyByteBuf != null) {
                bodyByteBuf.release();
            }
        }
    }

    @Override
    public Request createRequest() {
        // tcp protocol implementation, http protocols should override this method
        return new RpcRequest();
    }

    @Override
    public Response createResponse() {
        // tcp protocol implementation, http protocols should override this method
        return new RpcResponse();
    }

    @Override
    public Request getRequest() {
        // tcp protocol implementation, http protocols should override this method
        Request request = RpcRequest.getRpcRequest();
        request.reset();
        return request;
    }

    @Override
    public Response getResponse() {
        // tcp protocol implementation, http protocols should override this method
        Response response = RpcResponse.getRpcResponse();
        response.reset();
        return response;
    }

    @Override
    public boolean isCoexistence() {
        return true;
    }

    public byte[] encodeRequestBody(Request request, RpcMethodInfo rpcMethodInfo) {
        Validate.notNull(request, "body must not be empty");

        SPBody spBody = new SPBody();
        spBody.setServiceName(request.getServiceName());
        spBody.setMethodName(request.getMethodName());
        spBody.setParameters(request.getArgs());
        byte[] bytes;

        Schema<SPBody> schema = RuntimeSchema.getSchema(SPBody.class);
        bytes = ProtobufIOUtil.toByteArray(spBody, schema, LinkedBuffer.allocate(500));

        return bytes;
    }

    public byte[] encodeResponseBody(Object result, RpcMethodInfo rpcMethodInfo) {
        Validate.notNull(result, "body must not be empty");

        SPBody spBody = new SPBody();
        spBody.setServiceName(rpcMethodInfo.getServiceName());
        spBody.setMethodName(rpcMethodInfo.getMethodName());
        spBody.setContent(result);
        byte[] bytes;
        Schema<SPBody> schema = RuntimeSchema.getSchema(SPBody.class);
        bytes = ProtobufIOUtil.toByteArray(spBody, schema, LinkedBuffer.allocate(500));

        return bytes;
    }

    //    public Object decodeResponseBody(ByteBuf body) {
    //
    //        return body.getContent();
    //    }

    public void decodeRequestBody(SPBody body, Request request) {
        String serviceName = body.getServiceName();
        String methodName = body.getMethodName();
        RpcMethodInfo rpcMethodInfo = ServiceManager.getInstance().getService(serviceName, methodName);
        Validate.notNull(rpcMethodInfo,
                "find no method provider for service:" + serviceName + " , method:" + methodName);
        request.setArgs(body.getParameters());
        request.setMethodName(methodName);
        request.setRpcMethodInfo(rpcMethodInfo);
        request.setTarget(rpcMethodInfo.getTarget());
        request.setTargetMethod(rpcMethodInfo.getMethod());
        //request.setMsg(requestPacket);
        //request.setKvAttachment(requestPacket.getAttachments());
    }

    // sp head

    @Override
    public SPHead createSPHead() {
        return new DefaultSPHead();
    }

    @Override
    public DefaultSPHead headFromByteBuf(ByteBuf buf) throws BadSchemaException {
        DefaultSPHead head = new DefaultSPHead();
        if (buf.readableBytes() < SPHEAD_LENGTH) {
            throw new IllegalArgumentException("not enough bytes to read");
        }
        head.id = buf.readShortLE();
        head.version = buf.readShortLE();
        head.logId = buf.readLongLE();
        byte[] bytes = new byte[PROVIDER_LENGTH];
        buf.readBytes(bytes);
        int n = 0;
        while (n < bytes.length && bytes[n] != 0) {
            n++;
        }
        head.provider = new String(bytes, 0, n);
        head.magicNumber = buf.readIntLE();
        if (head.magicNumber != SPHEAD_MAGIC_NUM) {
            throw new BadSchemaException("nshead magic number does not match");
        }
        head.type = buf.readIntLE();
        head.bodyLength = buf.readIntLE();
        return head;
    }

    @Override
    public byte[] headToBytes(SPHead spHead) {
        DefaultSPHead usedSpHead = (DefaultSPHead) spHead;
        ByteBuf buffer = Unpooled.buffer(SPHEAD_LENGTH);
        buffer.writeShortLE(usedSpHead.id);
        buffer.writeShortLE(usedSpHead.version);
        buffer.writeLongLE(usedSpHead.logId);
        byte[] providerBytes = usedSpHead.provider.getBytes();

        if (providerBytes.length >= PROVIDER_LENGTH) {
            buffer.writeBytes(providerBytes, 0, PROVIDER_LENGTH);
        } else {
            buffer.writeBytes(providerBytes, 0, providerBytes.length);
            buffer.writeBytes(DefaultSPHead.ZEROS, 0, PROVIDER_LENGTH - providerBytes.length);
        }
        buffer.writeIntLE(usedSpHead.magicNumber);
        buffer.writeIntLE(usedSpHead.type);
        buffer.writeIntLE(usedSpHead.bodyLength);
        return buffer.array();
        //        // buffer.order()
        //
        //        ByteBuffer buf = ByteBuffer.allocate(SPHEAD_LENGTH);
        //        buf.order(ByteOrder.LITTLE_ENDIAN);
        //        buf.putShort(usedSpHead.id);
        //        buf.putShort(usedSpHead.version);
        //        buf.putLong(usedSpHead.logId);
        //        byte[] providerBytes = usedSpHead.provider.getBytes();
        //        if (providerBytes.length >= PROVIDER_LENGTH) {
        //            buf.put(providerBytes, 0, PROVIDER_LENGTH);
        //        } else {
        //            buf.put(providerBytes, 0, providerBytes.length);
        //            buf.put(DefaultSPHead.ZEROS, 0, PROVIDER_LENGTH - providerBytes.length);
        //        }
        //        buf.putInt(usedSpHead.magicNumber);
        //        buf.putInt(usedSpHead.type);
        //        buf.putInt(usedSpHead.bodyLength);
        //        return buf.array();
    }

}
