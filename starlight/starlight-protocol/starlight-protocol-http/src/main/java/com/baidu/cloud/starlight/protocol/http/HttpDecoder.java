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
 
package com.baidu.cloud.starlight.protocol.http;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpMessage;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Decode ByteBuf to MsgBase, use {@link com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObjectDecoder} Decode
 * msg body: from bytes to pojo. Created by liuruisen on 2020/5/27.
 */
public abstract class HttpDecoder implements ProtocolDecoder {

    protected static final Logger LOGGER = LoggerFactory.getLogger(HttpDecoder.class);

    @Override
    public MsgBase decode(DynamicCompositeByteBuf input) throws CodecException {

        if (input.readableBytes() <= 0) {
            throw new CodecException(CodecException.PROTOCOL_INSUFFICIENT_DATA_EXCEPTION,
                "Too little data to parse using Http"); // wait and retry
        }
        // 解析请求或者响应行，能解出来证明是Http协议的数据，不能解出来证明不是

        // NOTICE 暂无办法复现、构造如下异常:
        // NOTICE PROTOCOL_INSUFFICIENT_DATA_EXCEPTION PROTOCOL_DECODE_NOTMATCH_EXCEPTION PROTOCOL_DECODE_EXCEPTION
        MsgBase msgBase = null;
        ByteBuf byteBuf = input.retainedSlice(input.readableBytes());
        FullHttpMessage httpMessage = null;
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestResponseDecoder());
        try {
            embeddedChannel.writeInbound(byteBuf);
            httpMessage = embeddedChannel.readInbound();
            if (httpMessage == null || !httpMessage.decoderResult().isSuccess()) {
                LOGGER.debug("Cannot use Http protocol to decode: decoded result is null or failed");
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Cannot use Http protocol to decode: decoded result is null");
            }

            if (httpMessage.headers() != null) {
                // request id check, discard
                if (httpMessage.headers().get(AbstractHttpProtocol.X_STARLIGHT_ID) == null) {
                    // support null id for url request (2020.10.16 for ironman)
                    httpMessage.headers().add(AbstractHttpProtocol.X_STARLIGHT_ID, "-1"); // -1 represent none
                    LOGGER.debug("Starlight id is null, make sure it is correct");
                }

                // content type check
                String contentTypeAndEncoding = httpMessage.headers().get(HttpHeaderNames.CONTENT_TYPE);
                if (!StringUtils.isBlank(contentTypeAndEncoding)) {
                    String[] splits = contentTypeAndEncoding.split(";");
                    String contentType = splits[0];
                    // not support content_type, discard
                    if (!contentType.equals(AbstractHttpProtocol.CONTENT_TYPE_JSON)) {

                        /*
                         * httpMessage = null; input.skipBytes(byteBuf.readerIndex()); throw new
                         * CodecException(CodecException.PROTOCOL_DECODE_EXCEPTION,
                         * "Cannot use httpDecoder to decode bytes");
                         */
                        // this protocol can only deal with http protobuf and http json request.
                        // record
                        LOGGER.warn("ContentType id is not application/json, make sure it is a browser request");
                    }
                }
            } else {
                LOGGER.error("The http request not have headers, this is unlikely to happen");
            }

            if (httpMessage instanceof FullHttpRequest) {
                msgBase = reverseConvertRequest((FullHttpRequest) httpMessage);
            }

            if (httpMessage instanceof FullHttpResponse) {
                msgBase = reverseConvertResponse((FullHttpResponse) httpMessage);
            }
        } finally {
            // When there is only one http protocol, and httpMessage is not null, we think decode success and skip bytes
            // FIXME When multiple http protocols are implemented, please check if the logic here is as expected
            if (httpMessage != null && httpMessage.decoderResult().isSuccess() /* && msgBase != null */) {
                input.skipBytes(byteBuf.readerIndex());
            }
            LOGGER.debug("input.retainedSlice bytebuf refCnt {}", byteBuf.refCnt());
            byteBuf.release();
            embeddedChannel.close();
        }

        return msgBase;
    }

    @Override
    public void decodeBody(MsgBase msgBase) throws CodecException {
        if (msgBase == null) {
            throw new CodecException(CodecException.BODY_DECODE_EXCEPTION, "Message is null to decode");
        }

        if (msgBase instanceof Request) {
            decodeRequestBody((Request) msgBase);
        }

        if (msgBase instanceof Response) {
            decodeResponseBody((Response) msgBase);
        }
    }

    private void decodeRequestBody(Request request) {
        if (request.getParams() == null || request.getParams().length == 0) {
            return;
        }

        // FIXME generic调用
        if (request.getParamsTypes() == null) { // paramTypes is get from target Method when Processor#process
            throw new CodecException(CodecException.BODY_DECODE_EXCEPTION, "Body data type is null");
        }

        for (int i = 0; i < request.getParams().length; i++) { // find byte[] params and de serialize
            if (request.getParams()[i] instanceof byte[]) { // TODO check and vertify
                byte[] bodyBytes = (byte[]) request.getParams()[i];
                if (bodyBytes != null && bodyBytes.length > 0) {
                    Type bodyType = request.getGenericParamsTypes()[i];
                    Serializer serializer = serializer(request.getProtocolName());
                    Object bodyObj = serializer.deserialize(bodyBytes, bodyType);
                    request.getParams()[i] = bodyObj;
                    break; // only one requestBody
                }
            }
        }
    }

    private void decodeResponseBody(Response response) {
        // TODO generic call
        if (response.getBodyBytes() == null) {
            return;
        }
        Serializer serializer = serializer(response.getProtocolName());
        if (response.getStatus() != Constants.SUCCESS_CODE) { // error: response content is error message
            response.setErrorMsg((String) serializer.deserialize(response.getBodyBytes(), String.class));
        } else { // success: response content is response message
            if (response.getReturnType() == null) { // body type is get from target Method in ClientProcessor
                throw new CodecException(CodecException.BODY_DECODE_EXCEPTION, "Body data type is null");
            }
            response.setResult(serializer.deserialize(response.getBodyBytes(), response.getGenericReturnType()));
        }
    }

    /**
     * TODO 结合逻辑看下此部分是否可以抽象出公共逻辑，release的逻辑想办法移动到这里 Reverse convert {@link FullHttpRequest} to {@link RpcRequest} in
     * accordance with protocol and programming contract.
     *
     * @param httpRequest
     * @return
     */
    protected abstract Request reverseConvertRequest(FullHttpRequest httpRequest);

    /**
     * Reverse convert {@link FullHttpResponse} to {@link RpcResponse} in accordance with protocol and programming
     * contract.
     * <p>
     * Convert HttpResponse to Response
     * <ul>
     * <li>Step1: convert headers to kvMap, requestId</li>
     * <li>Step2: convert content to bodyBytes</li>
     * </ul>
     *
     * @param httpResponse
     * @return
     */
    protected Response reverseConvertResponse(FullHttpResponse httpResponse) {
        try {
            long corelationId = Long.parseLong(httpResponse.headers().get(AbstractHttpProtocol.X_STARLIGHT_ID));
            Response response = new RpcResponse(corelationId);
            // FIXME CompressType

            // set attachmentKv
            response.setAttachmentKv(new HashMap<>());
            for (Map.Entry<String, String> entry : httpResponse.headers().entries()) {
                response.getAttachmentKv().put(entry.getKey(), entry.getValue());
            }
            // set status
            response.setStatus(httpResponse.status().code());
            if (!httpResponse.status().equals(HttpResponseStatus.OK)) {
                LOGGER.warn("Id:{}, response status is {}", response.getId(), response.getStatus());
                response.setErrorMsg(
                    "Response is error, Id:" + response.getId() + ", response status is " + response.getStatus());
            }
            // set bodyBytes
            ByteBuf content = httpResponse.content();
            if (content != null && content.readableBytes() > 0) {
                byte[] contentBytes = new byte[content.readableBytes()];
                content.readBytes(contentBytes);
                response.setBodyBytes(contentBytes);
            }
            return response;
        } finally {
            httpResponse.release();
        }
    }

    protected Serializer serializer(String protocolName) {
        Protocol brpcProtocol = ExtensionLoader.getInstance(Protocol.class).getExtension(protocolName);
        return brpcProtocol.getSerialize();
    }
}
