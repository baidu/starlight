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

import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.api.utils.NetUriUtils;
import com.baidu.cloud.starlight.serialization.serializer.DyuProtostuffSerializer;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcBizException;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcException;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcExecutionException;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcMethodNotFountException;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcServiceNotFoundException;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcSystemException;
import com.baidu.fengchao.stargate.remoting.exceptions.RpcTimeoutException;

import java.util.HashMap;

/**
 * Created by liuruisen on 2020/7/20. Head is the lenght of body Body is {@link StargateResponse} or
 * {@link StargateResponse} serialized bytes +--------------------------------------+-------------------+ + header |
 * body | + 00000000 00000000 00000000 00000111 | 14 bytes body | + byte[3] byte[2] byte[1] byte[0] | message |
 * +--------------------------------------+-------------------+
 */
public class StargateEncoder implements ProtocolEncoder {

    /**
     * Same as {@link StarlightRpcException#SERVICE_NOT_FOUND_EXCEPTION}
     */
    private static final int SERVICE_NOT_FOUND_EXCEPTION = 1001;

    /**
     * Same as {@link StarlightRpcException#METHOD_NOT_FOUND_EXCEPTION}
     */
    private static final int METHOD_NOT_FOUND_EXCEPTION = 1002;

    /**
     * Same as {@link StarlightRpcException#BAD_REQUEST}
     */
    private static final int BAD_REQUEST = 1003;

    /**
     * Same as {@link StarlightRpcException#TIME_OUT_EXCEPTION}
     */
    private static final int TIME_OUT_EXCEPTION = 1008;

    /**
     * Same as {@link StarlightRpcException#BIZ_ERROR}
     */
    private static final int BIZ_ERROR = 2002;

    @Override
    public ByteBuf encode(MsgBase input) throws CodecException {
        if (input == null) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "MsgBase is null, cannot use stargate to encode");
        }

        if (input instanceof Request) {
            Request request = (Request) input;
            // head + body
            return Unpooled.wrappedBuffer(stargateHeader(request.getBodyBytes().length), request.getBodyBytes());
        }

        if (input instanceof Response) {
            Response response = (Response) input;
            // head + body
            return Unpooled.wrappedBuffer(stargateHeader(response.getBodyBytes().length), response.getBodyBytes());
        }

        throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
            "MsgBase type is illegal {" + input.getClass().getName() + "}, cannot use stargate to encode");
    }

    @Override
    public void encodeBody(MsgBase msgBase) throws CodecException {
        if (msgBase == null) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "MsgBase is null, cannot use stargate to encode");
        }

        if (msgBase instanceof Request) {
            encodeRequestBody((Request) msgBase);
        }

        if (msgBase instanceof Response) {
            encodeResponseBody((Response) msgBase);
        }
    }

    private void encodeRequestBody(Request request) {
        if (request.getServiceConfig() == null) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "Cannot use stargate to encode, group / version is null");
        }
        // stargate uri: carries some necessary information
        URI uri = new URI.Builder(StargateProtocol.PROTOCOL_NAME, NetUriUtils.getLocalHost(), 0)
            .param(Constants.GROUP_KEY, request.getServiceConfig().getGroup()) // default is "normal"
            .param(Constants.VERSION_KEY, request.getServiceConfig().getVersion()) // default is "1.0.0"
            // serviceName can be interfaceName or generic target interfaceName
            .param(Constants.INTERFACE_KEY, request.getServiceName())
            .param(Constants.GENERIC_KEY, GenericUtil.isGenericCall(request)) // support stargate generic filter
            .build();

        // starlight long type id will be cast to string as stargate String type id
        StargateRequest stargateRequest = new StargateRequest(String.valueOf(request.getId()), uri,
            request.getMethodName(), request.getParamsTypes(), request.getParams());

        stargateRequest.setAttachments(new HashMap<>(request.getAttachmentKv()));

        byte[] bodyBytes = serializer().serialize(stargateRequest, StargateRequest.class);
        request.setBodyBytes(bodyBytes);
    }

    private void encodeResponseBody(Response response) {
        StargateResponse stargateResponse = new StargateResponse(String.valueOf(response.getId()));
        // when use original stargate client call starlight server,stargate uuid is string type and
        // had be stored in request kv attachment by StargateDecoder#decodeRequest.
        // so we retain the String type uuid and set it to StargateResponse#id
        if (response.getRequest() != null && response.getRequest().getAttachmentKv() != null
            && response.getRequest().getAttachmentKv().get(Constants.STARGATE_UUID) != null) {
            stargateResponse.setId((String) response.getRequest().getAttachmentKv().get(Constants.STARGATE_UUID));
        }

        stargateResponse.setResult(response.getResult());
        stargateResponse.setAttachments(response.getAttachmentKv());
        if (response.getStatus() != Constants.SUCCESS_CODE) {
            stargateResponse.setException(generateStargateException(response));
        }
        byte[] bodyBytes = serializer().serialize(stargateResponse, StargateResponse.class);
        response.setBodyBytes(bodyBytes);
    }

    private DyuProtostuffSerializer serializer() {
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(StargateProtocol.PROTOCOL_NAME);
        return (DyuProtostuffSerializer) protocol.getSerialize();
    }

    private byte[] stargateHeader(int bodyLength) {
        return new byte[] {(byte) ((bodyLength >> 24) & 0xFF), (byte) ((bodyLength >> 16) & 0xFF),
            (byte) ((bodyLength >> 8) & 0xFF), (byte) (bodyLength & 0xFF),};
    }

    /**
     * 为使Starlight产生的异常可以被Stargate框架识别并展示出来，而增加的异常转换方法
     *
     * @param response
     * @return
     */
    private RpcException generateStargateException(Response response) {
        switch (response.getStatus()) {
            case SERVICE_NOT_FOUND_EXCEPTION:
                return new RpcServiceNotFoundException(response.getErrorMsg());
            case METHOD_NOT_FOUND_EXCEPTION:
                return new RpcMethodNotFountException(response.getErrorMsg());
            case BAD_REQUEST:
                return new RpcExecutionException(response.getErrorMsg());
            case TIME_OUT_EXCEPTION:
                return new RpcTimeoutException(response.getErrorMsg());
            case BIZ_ERROR:
                return new RpcBizException(response.getErrorMsg());
            default:
                return new RpcSystemException(response.getErrorMsg());
        }
    }
}
