package com.baidu.brpc.protocol.push;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.PushServerRpcFutureManager;
import com.baidu.brpc.server.ServiceManager;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unchecked")
public class ServerPushProtocol extends AbstractProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(ServerPushProtocol.class);
    protected String encoding = "utf-8";

    public ServerPushProtocol(String encoding) {
        if (encoding != null) {
            this.encoding = encoding;
        }
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        Validate.notEmpty(request.getArgs(), "args must not be empty");
        byte[] bodyBytes = encodeRequestBody(request, request.getRpcMethodInfo());
        SPHead spHead = request.getSpHead();
        Validate.notNull(bodyBytes);
        spHead.bodyLength = bodyBytes.length;
        spHead.logId = request.getLogId();
        byte[] nsHeadBytes = spHead.toBytes();
        return Unpooled.wrappedBuffer(nsHeadBytes, bodyBytes);
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
    public Response decodeServerPushResponse(Object in, ChannelHandlerContext ctx) throws Exception {
        ServerPushPacket packet = (ServerPushPacket) in;
        RpcResponse rpcResponse = new RpcResponse();
        // channel info是在客户端生成连接池的时候生成的
        // ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        Long logId = (long) packet.getSpHead().logId;

        RpcFuture future = PushServerRpcFutureManager.getInstance().removeRpcFuture(logId);
        rpcResponse.setLogId(logId);

        if (future == null) {
            return rpcResponse;
        }
        rpcResponse.setRpcFuture(future);

        Object responseBody = decodeResponseBody(packet.getSpBody());
        if (responseBody == null) {
            return null;
        }

        rpcResponse.setResult(responseBody);
        return rpcResponse;
    }

    @Override
    public Response decodeResponse(Object in, ChannelHandlerContext ctx) throws Exception {
        ServerPushPacket packet = (ServerPushPacket) in;
        RpcResponse rpcResponse = new RpcResponse();
        // channel info是在客户端生成连接池的时候生成的
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        Long logId = channelInfo.getLogId();
        if (packet.getSpHead().logId != 0) {
            logId = (long) packet.getSpHead().logId;
        }
        rpcResponse.setLogId(logId);
        RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
        if (future == null) {
            return rpcResponse;
        }
        rpcResponse.setRpcFuture(future);

        Object responseBody = decodeResponseBody(packet.getSpBody());
        if (responseBody == null) {
            return null;
        }

        rpcResponse.setResult(responseBody);
        return rpcResponse;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        byte[] bodyBytes = encodeResponseBody(response.getResult(), response.getRpcMethodInfo());
        SPHead nsHead = response.getSpHead();
        if (nsHead == null) {
            nsHead = new SPHead((int) response.getLogId(), bodyBytes.length);
        } else {
            nsHead.bodyLength = bodyBytes.length;
        }
        nsHead.logId = response.getLogId();
        nsHead.type = SPHead.TYPE_RESPONSE;
        byte[] headBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(headBytes, bodyBytes);
    }

    public String decodeClientNameByRegisterRequest(Object packet) {
        Request request = this.createRequest();
        ServerPushPacket nsHeadPacket = (ServerPushPacket) packet;
        String id = nsHeadPacket.getSpBody().getId();
        request.setLogId(StringUtils.isEmpty(id) ? (long) nsHeadPacket.getSpHead().logId : Long.parseLong(id));
        Object[] parameters = ((ServerPushPacket) packet).getSpBody().getParameters();

        return ((RegistryContent) parameters[0]).getClientName();
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = this.createRequest();
        ServerPushPacket nsHeadPacket = (ServerPushPacket) packet;
        SPBody spBody = nsHeadPacket.getSpBody();
        String id = spBody.getId();
        request.setLogId(StringUtils.isEmpty(id) ? (long) nsHeadPacket.getSpHead().logId : Long.parseLong(id));
        request.setSpHead(((ServerPushPacket) packet).getSpHead());
        decodeRequestBody((nsHeadPacket).getSpBody(), request);
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
    public ServerPushPacket decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < SPHead.SPHEAD_LENGTH) {
            throw notEnoughDataException;
        }
        ServerPushPacket packet = new ServerPushPacket();
        ByteBuf fixHeaderBuf = in.retainedSlice(SPHead.SPHEAD_LENGTH);
        try {

            SPHead nsHead = SPHead.fromByteBuf(fixHeaderBuf);
            packet.setSpHead(nsHead);
            int bodyLength = nsHead.bodyLength;

            // 512M
            if (bodyLength > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodyLength);
            }

            if (in.readableBytes() < SPHead.SPHEAD_LENGTH + bodyLength) {
                throw notEnoughDataException;
            }

            in.skipBytes(SPHead.SPHEAD_LENGTH);
            ByteBuf bodyBuf = in.readRetainedSlice(bodyLength);
            int readableBytes = bodyBuf.readableBytes();
            byte[] bodyBytes = new byte[readableBytes];
            bodyBuf.readBytes(bodyBytes);
            Schema<SPBody> schema = RuntimeSchema.getSchema(SPBody.class);
            SPBody spBody = new SPBody();
            ProtobufIOUtil.mergeFrom((byte[]) bodyBytes, spBody, schema);
            packet.setSpBody(spBody);
            return packet;
        } catch (Exception e) {
            LOG.error("error:", e);
            throw new RuntimeException("decode failed:" + e.getMessage());
        } finally {
            fixHeaderBuf.release();
        }
    }

    public byte[] encodeRequestBody(Request request, RpcMethodInfo rpcMethodInfo) {
        Validate.notNull(request, "body must not be empty");

        SPBody spBody = new SPBody();
        spBody.setServiceName(rpcMethodInfo.getServiceName());
        spBody.setMethodName(rpcMethodInfo.getMethodName());
        spBody.setParameters(request.getArgs());
        spBody.setId(String.valueOf(request.getLogId()));
        byte[] bytes;

        Schema<SPBody> schema = RuntimeSchema.getSchema(SPBody.class);
        bytes = ProtobufIOUtil.toByteArray(spBody, schema, LinkedBuffer.allocate(500));
        //        try {
        //            bytes = json.getBytes(this.encoding);
        //        } catch (Exception e) {
        //            log.error("can not serialize object using gson", e);
        //            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
        //        }

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
        //        String json = gson.toJson(spBody);
        //        try {
        //            bytes = json.getBytes(this.encoding);
        //        } catch (Exception e) {
        //            log.error("can not serialize object using gson", e);
        //            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
        //        }

        return bytes;
    }

    public Object decodeResponseBody(SPBody body) {
        return body.getContent();
    }

    public void decodeRequestBody(SPBody body, Request request) {
        String serviceName = body.getServiceName();
        String methodName = body.getMethodName();
        RpcMethodInfo rpcMethodInfo = ServiceManager.getInstance().getService(serviceName, methodName);

        request.setArgs(body.getParameters());
        request.setMethodName(methodName);
        request.setRpcMethodInfo(rpcMethodInfo);
        request.setTarget(rpcMethodInfo.getTarget());
        request.setTargetMethod(rpcMethodInfo.getMethod());
        //request.setMsg(requestPacket);
        //request.setKvAttachment(requestPacket.getAttachments());
    }

    //    public Object decodeRequestBody(ByteBuf bodyBuf, RpcMethodInfo rpcMethodInfo) {
    //        try {
    //            Object result;
    //            try {
    //                int readableBytes = bodyBuf.readableBytes();
    //                byte[] bodyBytes = new byte[readableBytes];
    //                bodyBuf.readBytes(bodyBytes);
    //                String jsonString = new String(bodyBytes, this.encoding);
    //                if (rpcMethodInfo.getTarget() != null) {
    //                    // server端
    //                    result = gson.fromJson(jsonString, rpcMethodInfo.getInputClasses()[0]);
    //                } else {
    //                    result = gson.fromJson(jsonString, rpcMethodInfo.getOutputClass());
    //                }
    //            } catch (Exception e) {
    //                log.error("can not deserialize object", e);
    //                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
    //            }
    //            return result;
    //        } finally {
    //            if (bodyBuf != null) {
    //                bodyBuf.release();
    //            }
    //        }
    //    }

}
