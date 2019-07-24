package com.baidu.brpc.protocol.pbrpc;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.compress.Compress;
import com.baidu.brpc.compress.CompressManager;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.BaiduRpcErrno;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.protocol.nshead.NSHead;
import com.baidu.brpc.protocol.pbrpc.PublicPbrpcProto.PublicPbrpcRequest;
import com.baidu.brpc.protocol.pbrpc.PublicPbrpcProto.PublicPbrpcResponse;
import com.baidu.brpc.protocol.pbrpc.PublicPbrpcProto.RequestBody;
import com.baidu.brpc.protocol.pbrpc.PublicPbrpcProto.RequestHead;
import com.baidu.brpc.protocol.pbrpc.PublicPbrpcProto.ResponseBody;
import com.baidu.brpc.protocol.pbrpc.PublicPbrpcProto.ResponseHead;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.ProtobufUtils;
import com.baidu.brpc.utils.RpcMetaUtils;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.tools.ant.util.DateUtils;

import java.net.InetAddress;
import java.util.Calendar;

@Slf4j
public class PublicPbrpcProtocol extends AbstractProtocol {

    private static final String VERSION = "pbrpc=1.0";
    private static final String CHARSET = "utf-8";
    private static final String SUCCESS_TEXT = "success";
    private static final String TIME_FORMAT = "%Y%m%d%H%M%S";
    private static final String PROVIDER = "__pbrpc__";
    private static final int CONTENT_TYPE = 1;
    private static final int COMPRESS_TYPE = 1;
    private static final int NSHEAD_VERSION = 1000;

    private static final CompressManager compressManager = CompressManager.getInstance();

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {

        // service method
        RpcMetaUtils.RpcMetaInfo rpcMetaInfo = RpcMetaUtils.parseRpcMeta(request.getTargetMethod());
        int methodIndex;
        try {
            methodIndex = Integer.valueOf(rpcMetaInfo.getMethodName());
        } catch (NumberFormatException ex) {
            String errorMsg = "methodName must be integer when using pbrpc, "
                    + "it is equal to proto method sequence from 0";
            log.warn(errorMsg);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, errorMsg, ex);
        }

        // build head
        RequestHead.Builder headBuilder = RequestHead.newBuilder();
        headBuilder.setFromHost(InetAddress.getLocalHost().getHostAddress());
        headBuilder.setContentType(CONTENT_TYPE);
        // todo short connection
        headBuilder.setConnection(false);
        headBuilder.setCharset(CHARSET);
        headBuilder.setCreateTime(DateUtils.format(Calendar.getInstance().getTime(), TIME_FORMAT));
        headBuilder.setLogId(request.getLogId());
        headBuilder.setCompressType(request.getCompressType());

        // build body
        RequestBody.Builder bodyBuilder = RequestBody.newBuilder();
        bodyBuilder.setVersion(VERSION);
        bodyBuilder.setCharset(CHARSET);
        bodyBuilder.setService(request.getServiceName());
        bodyBuilder.setMethodId(methodIndex);
        bodyBuilder.setId(request.getLogId());

        Compress compress = compressManager.getCompress(request.getCompressType());
        ByteBuf protoBuf = compress.compressInput(request.getArgs()[0], request.getRpcMethodInfo());
        bodyBuilder.setSerializedRequest(ByteString.copyFrom(protoBuf.nioBuffer()));

        PublicPbrpcRequest pbreq = PublicPbrpcRequest.newBuilder()
                .setRequestHead(headBuilder.build())
                .addRequestBody(bodyBuilder.build())
                .build();

        NSHead nsHead = new NSHead();
        nsHead.logId = (int) request.getLogId();
        nsHead.magicNumber = NSHead.NSHEAD_MAGIC_NUM;
        nsHead.provider = PROVIDER;
        nsHead.version = NSHEAD_VERSION;
        nsHead.bodyLength = pbreq.getSerializedSize();

        byte[] nsHeadBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(nsHeadBytes, request.getRpcMethodInfo().inputEncode(pbreq));

    }


    @Override
    public PublicPbRpcPacket decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in,
                                    boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < NSHead.NSHEAD_LENGTH) {
            throw notEnoughDataException;
        }
        PublicPbRpcPacket packet = new PublicPbRpcPacket();
        ByteBuf fixHeaderBuf = in.retainedSlice(NSHead.NSHEAD_LENGTH);
        try {
            NSHead nsHead = NSHead.fromByteBuf(fixHeaderBuf);
            packet.setNsHead(nsHead);
            int bodyLength = nsHead.bodyLength;

            // 512M
            if (bodyLength > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodyLength);
            }

            if (in.readableBytes() < NSHead.NSHEAD_LENGTH + bodyLength) {
                throw notEnoughDataException;
            }

            in.skipBytes(NSHead.NSHEAD_LENGTH);
            ByteBuf bodyBuf = in.readRetainedSlice(bodyLength);
            packet.setBody(bodyBuf);
            return packet;
        } finally {
            fixHeaderBuf.release();
        }
    }


    @Override
    public Response decodeResponse(Object in, ChannelHandlerContext ctx) throws Exception {
        PublicPbRpcPacket packet = (PublicPbRpcPacket) in;

        RpcResponse rpcResponse = new RpcResponse();
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());

        ByteBuf bodyBuf = packet.getBody();
        try {
            PublicPbrpcResponse pbResponse = (PublicPbrpcResponse) ProtobufUtils.parseFrom(bodyBuf,
                    PublicPbrpcResponse.getDefaultInstance());

            ResponseBody body = pbResponse.getResponseBody(0);
            ResponseHead head = pbResponse.getResponseHead();

            if (head.getCode() != 0) {
                rpcResponse.setException(new RpcException(head.getText()));
            } else {

                rpcResponse.setLogId(body.getId());
                RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
                if (future == null) {
                    return rpcResponse;
                }
                rpcResponse.setRpcFuture(future);

                Object responseBody;
                int compressType = head.getCompressType();
                try {
                    Compress compress = compressManager.getCompress(compressType);
                    responseBody = compress.uncompressOutput(body.getSerializedResponse().toByteArray(),
                            future.getRpcMethodInfo());

                } catch (Exception ex) {
                    String errorMsg = String.format("decode failed, msg=%s", ex.getMessage());
                    log.error(errorMsg);
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, errorMsg, ex);
                }

                rpcResponse.setResult(responseBody);
            }

        } finally {
            if (bodyBuf != null) {
                bodyBuf.release();
            }
        }

        return rpcResponse;
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = this.getRequest();

        PublicPbRpcPacket pbPacket = (PublicPbRpcPacket) packet;
        ByteBuf bodyBuf = pbPacket.getBody();
        try {
            PublicPbrpcRequest pbRequest = (PublicPbrpcRequest) ProtobufUtils.parseFrom(bodyBuf,
                    PublicPbrpcRequest.getDefaultInstance());

            RequestBody body = pbRequest.getRequestBody(0);
            RequestHead head = pbRequest.getRequestHead();

            request.setLogId(body.getId());
            int compressType = head.getCompressType();
            request.setCompressType(compressType);

            // service info
            ServiceManager serviceManager = ServiceManager.getInstance();
            RpcMethodInfo rpcMethodInfo = serviceManager.getService(body.getService(),
                    String.valueOf(body.getMethodId()));
            if (rpcMethodInfo == null) {
                String errorMsg = String.format("Fail to find service=%s, methodIndex=%s", body.getService(),
                        body.getMethodId());
                request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
                return request;
            }
            request.setRpcMethodInfo(rpcMethodInfo);
            request.setTargetMethod(rpcMethodInfo.getMethod());
            request.setTarget(rpcMethodInfo.getTarget());

            // proto body
            try {
                Compress compress = compressManager.getCompress(compressType);
                Object requestProto = compress.uncompressInput(body.getSerializedRequest().toByteArray(),
                        rpcMethodInfo);
                request.setArgs(new Object[] {requestProto});

            } catch (Exception ex) {
                String errorMsg = String.format("decode failed, msg=%s", ex.getMessage());
                log.error(errorMsg);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, errorMsg, ex);
            }
            return request;
        } finally {
            if (bodyBuf != null) {
                bodyBuf.release();
            }
        }

    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {

        ResponseHead.Builder headBuilder = ResponseHead.newBuilder();
        headBuilder.setFromHost(InetAddress.getLocalHost().getHostAddress());

        ResponseBody.Builder bodyBuilder = ResponseBody.newBuilder();

        bodyBuilder.setVersion(VERSION);
        bodyBuilder.setId(request.getLogId());
        if (response.getException() != null) {
            headBuilder.setCode(BaiduRpcErrno.Errno.EINTERNAL_VALUE);
            headBuilder.setText(response.getException().getMessage());
        } else {
            headBuilder.setCode(0);
            headBuilder.setText(SUCCESS_TEXT);
            headBuilder.setCompressType(request.getCompressType());

            Compress compress = compressManager.getCompress(request.getCompressType());
            ByteBuf responseProtoBuf = compress.compressOutput(
                    response.getResult(), response.getRpcMethodInfo());

            bodyBuilder.setSerializedResponse(ByteString.copyFrom(responseProtoBuf.nioBuffer()));

        }

        PublicPbrpcResponse pbres = PublicPbrpcResponse.newBuilder()
                .setResponseHead(headBuilder.build())
                .addResponseBody(bodyBuilder.build())
                .build();

        // nshead
        NSHead nsHead = new NSHead();
        nsHead.logId = (int) request.getLogId();
        nsHead.magicNumber = NSHead.NSHEAD_MAGIC_NUM;
        nsHead.provider = PROVIDER;
        nsHead.version = NSHEAD_VERSION;
        nsHead.bodyLength = pbres.getSerializedSize();

        byte[] nsHeadBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(nsHeadBytes, request.getRpcMethodInfo().inputEncode(pbres));

    }

    @Override
    public boolean isCoexistence() {
        return false;
    }
}
