/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.protocol.stargate;

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
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.NetUtils;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Stargate Byte format
 * head = body体长度，使用一个4位byte表示
 * +--------------------------------------+----------------+
 * + 00000000 00000000 00000000 00000111 |    14 bytes    |
 * + byte[3]  byte[2]  byte[1]  byte[0] | "HELLO, WORLD" |
 * +-----------------------------------+----------------+
 */
@Slf4j
public class StargateRpcProtocol extends AbstractProtocol {

    private static final int FIXED_HEAD_LEN = 4;

    private static final NotEnoughDataException notEnoughDataException
            = new NotEnoughDataException("Stargate not enough data");

    private static final String SERIALIZATION_EXCEPTION = "decode error,this problem is usually caused by"
            + "\n 1: difference of api.jar between server and client."
            + "\n 2: server do not catch Exception."
            + "\n 3: API contains a type that Stargate does not support. eg:HashMap.keySet()";

    private ServiceManager serviceManager = ServiceManager.getInstance();
    private boolean init = false;

    public void initEnv() {
        // init Stargate protoStuff
        // docs http://javadox.com/io.protostuff/protostuff-runtime/1.3.8/io/protostuff/runtime/RuntimeEnv.html
        if (!init) {
            synchronized (StargateRpcProtocol.class) {
                if (!init) {
                    System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
                    System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
                    System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
                    init = true;
                }
            }
        }
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws TooBigDataException, NotEnoughDataException, BadSchemaException {
        if (in.readableBytes() < FIXED_HEAD_LEN) {
            throw notEnoughDataException;
        }
        ByteBuf head = in.retainedSlice(FIXED_HEAD_LEN);
        try {
            int bodySize = head.readInt();
            if (in.readableBytes() < bodySize + FIXED_HEAD_LEN) {
                throw notEnoughDataException;
            }

            // 512M
            if (bodySize > 512 * 1024 * 1024) {
                throw new TooBigDataException("StarGate too big body size:" + bodySize);
            }

            byte[] body = new byte[bodySize];
            in.skipBytes(FIXED_HEAD_LEN);
            in.readBytes(body);
            return body;
        } finally {
            head.release();
        }
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        StargateURI stargateURI = new StargateURI
                .Builder("star", NetUtils.getLogHost(), 0)
                .param("version", request.getSubscribeInfo().getVersion())
                .param("group", request.getSubscribeInfo().getGroup())
                .param("interface", request.getSubscribeInfo().getInterfaceName())
                .param("consumer.id", UUID.randomUUID().toString())
                .build();

        Method method = request.getTargetMethod();
        StargateRpcRequestPacket requestPacket = new StargateRpcRequestPacket(
                stargateURI,
                method.getName(),
                method.getParameterTypes(),
                request.getArgs());
        requestPacket.setId(request.getLogId() + "");

        if (request.getKvAttachment() != null && !request.getKvAttachment().isEmpty()) {
            requestPacket.setAttachments(request.getKvAttachment());
        }

        try {
            initEnv();
            Schema<StargateRpcRequestPacket> schema = RuntimeSchema.getSchema(StargateRpcRequestPacket.class);
            byte[] body = ProtobufIOUtil.toByteArray(requestPacket, schema, LinkedBuffer.allocate(500));
            byte[] head = buildHead(body);
            return Unpooled.wrappedBuffer(head, body);
        } catch (Exception e) {
            log.warn(SERIALIZATION_EXCEPTION, e);
            throw new BadSchemaException(SERIALIZATION_EXCEPTION, e);
        }
    }

    /**
     * 客户端解码响应操作
     * PS：
     * BRPC 有且仅有当BRPC作为客户端时，使用FastFuture的LogId 作为线程绑定ID
     */
    @Override
    public Response decodeResponse(Object msg, ChannelHandlerContext ctx) throws Exception {
        try {
            StargateRpcResponsePacket rpcResponse = new StargateRpcResponsePacket();
            Schema<StargateRpcResponsePacket> schema = RuntimeSchema.getSchema(StargateRpcResponsePacket.class);
            ProtobufIOUtil.mergeFrom((byte[]) msg, rpcResponse, schema);
            try {
                Response response = new RpcResponse();
                response.setResult(rpcResponse.getResult());
                long logId = Long.parseLong(rpcResponse.getId());
                ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
                RpcFuture future = channelInfo.removeRpcFuture(logId);
                if (future == null) {
                    return response;
                }
                response.setRpcFuture(future);
                response.setLogId(logId);
                response.setKvAttachment(rpcResponse.getAttachments());
                return response;
            } catch (NumberFormatException n) {
                log.error(" when brpc is Client unexpect logId at {}", rpcResponse.getId(), n);
                throw new BadSchemaException(SERIALIZATION_EXCEPTION, n);
            }
        } catch (Exception e) {
            log.warn(SERIALIZATION_EXCEPTION, e);
            throw new BadSchemaException(SERIALIZATION_EXCEPTION, e);
        }
    }

    @Override
    public Request decodeRequest(Object packet) throws BadSchemaException {
        try {
            StargateRpcRequestPacket requestPacket = new StargateRpcRequestPacket();
            initEnv();
            Schema<StargateRpcRequestPacket> schema = RuntimeSchema.getSchema(StargateRpcRequestPacket.class);
            ProtobufIOUtil.mergeFrom((byte[]) packet, requestPacket, schema);

            String serviceName = requestPacket.getUri().getServiceName();
            String methodName = requestPacket.getMethodName();
            RpcMethodInfo rpcMethodInfo = serviceManager.getService(serviceName, methodName);

            Request request = new RpcRequest();
            request.setArgs(requestPacket.getParameters());
            request.setMethodName(methodName);
            request.setRpcMethodInfo(rpcMethodInfo);
            request.setTarget(rpcMethodInfo.getTarget());
            request.setTargetMethod(rpcMethodInfo.getMethod());
            request.setMsg(requestPacket);
            request.setKvAttachment(requestPacket.getAttachments());
            return request;
        } catch (Exception e) {
            log.error(" stargate decodeRequest error at {} ", e.getMessage(), e);
            throw new BadSchemaException(SERIALIZATION_EXCEPTION, e);
        }
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        try {
            StargateRpcRequestPacket stargateRpcRequestPacket = (StargateRpcRequestPacket) request.getMsg();
            StargateRpcResponsePacket responsePacket = new StargateRpcResponsePacket(
                    stargateRpcRequestPacket.getId(),
                    response.getResult(),
                    response.getException());

            if (response.getKvAttachment() != null && !response.getKvAttachment().isEmpty()) {
                responsePacket.setAttachments(response.getKvAttachment());
            }

            Schema<StargateRpcResponsePacket> schema = RuntimeSchema.getSchema(StargateRpcResponsePacket.class);
            byte[] body = ProtobufIOUtil.toByteArray(responsePacket, schema, LinkedBuffer.allocate(500));
            byte[] head = buildHead(body);

            return Unpooled.wrappedBuffer(head, body);
        } catch (Exception e) {
            log.warn(SERIALIZATION_EXCEPTION, e);
            throw new BadSchemaException(SERIALIZATION_EXCEPTION, e);
        }
    }

    /**
     * 根据Body的长度，生产4位数组的Head
     *
     * @param body 二进制Body
     */
    private byte[] buildHead(byte[] body) {
        int length = body.length;
        return new byte[]{
                (byte) ((length >> 24) & 0xFF),
                (byte) ((length >> 16) & 0xFF),
                (byte) ((length >> 8) & 0xFF),
                (byte) (length & 0xFF)
        };
    }

}
