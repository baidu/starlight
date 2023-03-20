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
 
package com.baidu.cloud.starlight.protocol.stargate;

import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.serialization.serializer.DyuProtostuffSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by liuruisen on 2020/7/20.
 */
public class StargateDecoder implements ProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(StargateDecoder.class);

    private static final ThreadLocal<String> messageType = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return "";
        }
    };

    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";

    @Override
    public MsgBase decode(DynamicCompositeByteBuf input) throws CodecException {

        if (input.readableBytes() < StargateProtocol.FIXED_LEN) {
            throw new CodecException(CodecException.PROTOCOL_INSUFFICIENT_DATA_EXCEPTION,
                "Too little data to parse using stargate"); // wait and retry
        }

        // NOTICE stargate协议中并没有magic num等能明确标识是本协议的字段，
        ByteBuf head = input.retainedSlice(StargateProtocol.FIXED_LEN);
        ByteBuf body = null;
        MsgBase msgBase = null;
        try {
            int bodySize = 0; // bodySize
            try {
                bodySize = head.readInt();
            } catch (Exception e) {
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Cannot parse first 32bit of data flow to int value using stargate protocol,"
                        + " maybe not bytes of stargate");
            }

            if (bodySize <= 0 || bodySize > StargateProtocol.MAX_BODY_SIZE) {
                // if head.readInt > MAX_BODY_SIZE or head.readInt <= 0,
                // we first think bytebuf is not stargate message.
                // Considering maybe is not stargate data flow
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Body size is bigger than 512m or less than 1, maybe not bytes of stargate");
            }
            // first byte of stargate data flow
            if (head.readByte() != StargateProtocol.FIRST_BYTE_VALUE_OF_BODY) {
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "The first byte of data flow is not equals FIRST_BYTE_VALUE_OF_STARGATE_BODY,"
                        + " not bytebuf of stargate");
            }

            if (input.readableBytes() < StargateProtocol.HEAD_LEN + bodySize) {
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTENOUGHDATA_EXCEPTION,
                    "Body Data not enough to parse using stargate"); // wait and retry
            }
            input.skipBytes(StargateProtocol.HEAD_LEN); // skip header size

            // body bytes
            body = input.readRetainedSlice(bodySize);
            byte[] bodyBytes = new byte[bodySize];
            body.readBytes(bodyBytes);

            // Prefer the last analyzed type, so we can improve performance
            switch (messageType.get()) {
                case REQUEST:
                    try {
                        return decodeRequest(bodyBytes);
                    } catch (CodecException e) {
                        messageType.set("");
                        LOGGER.info("[Stargate messageTyp detect] "
                            + "The body data flow cannot be decoded as {stargate request}, "
                            + "will try decoded as stargate response");
                    }
                    break;
                case RESPONSE:
                    try {
                        return decodeResponse(bodyBytes);
                    } catch (CodecException e) {
                        messageType.set("");
                        LOGGER.info("[Stargate messageType detect] "
                            + "The body data flow cannot be decoded as {stargate response}, "
                            + "will try decoded as stargate request");
                    }
                    break;
            }

            // try decode request
            try {
                msgBase = decodeRequest(bodyBytes);
                messageType.set(REQUEST);
                return msgBase;
            } catch (CodecException e) {
                LOGGER.info(
                    "[Stargate messageType detect] " + "The body data flow cannot be decoded as stargate request, "
                        + "will try decoded as stargate response, cause by {}",
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            }
            // try decode response
            try {
                msgBase = decodeResponse(bodyBytes);
                messageType.set(RESPONSE);
                return msgBase;
            } catch (CodecException e) {
                LOGGER.info(
                    "[Stargate messageType detect] "
                        + "The body data flow also cannot be decoded as stargate response, "
                        + "maybe not stargate messages or cannot deserialize, cause by {}",
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "The body data flow neither cannot be decode as stargate response"
                        + " nor be decode as stargate response,"
                        + " maybe not stargate messages or cannot deserialize");
            }
        } finally {
            head.release();
            if (body != null) {
                body.release();
            }
        }
    }

    private Request decodeRequest(byte[] bodyBytes) {
        StargateRequest stargateRequest = (StargateRequest) serializer().deserialize(bodyBytes, StargateRequest.class);

        if (StringUtils.isEmpty(stargateRequest.getId()) || StringUtils.isEmpty(stargateRequest.getMethodName())
            || stargateRequest.getUri() == null) {
            throw new CodecException("Illegal message: stargate request's is or methodName or uri is empty");
        }

        URI uri = stargateRequest.getUri();
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setVersion(uri.getParameter(Constants.VERSION_KEY));
        serviceConfig.setGroup(uri.getParameter(Constants.GROUP_KEY));

        Request request = new RpcRequest();
        request.setMethodName(stargateRequest.getMethodName());
        request.setServiceConfig(serviceConfig);
        request.setParams(stargateRequest.getParameters());
        request.setParamsTypes(stargateRequest.getParameterTypes());
        request.setAttachmentKv(stargateRequest.getAttachments());
        request.setProtocolName(StargateProtocol.PROTOCOL_NAME);
        if (request.getAttachmentKv() != null) {
            // stargate id is string, but starlight id is long
            // so we use kv map to restore stargate uuid
            request.getAttachmentKv().put(Constants.STARGATE_UUID, stargateRequest.getId());
        }
        // starlight not support group & version, so we use interfaceName to invoke
        request.setServiceName(uri.getParameter(Constants.INTERFACE_KEY));

        return request;
    }

    private Response decodeResponse(byte[] bodyBytes) {
        StargateResponse stargateResponse =
            (StargateResponse) serializer().deserialize(bodyBytes, StargateResponse.class);

        if (StringUtils.isEmpty(stargateResponse.getId())) {
            throw new CodecException("Illegal message: stargate response's id is empty");
        }

        Response response = new RpcResponse(Long.parseLong(stargateResponse.getId()));
        response.setResult(stargateResponse.getResult());
        response.setProtocolName(StargateProtocol.PROTOCOL_NAME);
        response.setAttachmentKv(stargateResponse.getAttachments());
        response.setStatus(Constants.SUCCESS_CODE);
        if (stargateResponse.getException() != null) {
            response.setErrorMsg("Server had occur exception: " + stargateResponse.getException().getMessage());
            response.setStatus(StarlightRpcException.BIZ_ERROR);
        }

        return response;
    }

    @Override
    public void decodeBody(MsgBase msgBase) throws CodecException {
        // do nothing, because we had done decode work completely in io thread
    }

    private DyuProtostuffSerializer serializer() {
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(StargateProtocol.PROTOCOL_NAME);
        return (DyuProtostuffSerializer) protocol.getSerialize();
    }
}
