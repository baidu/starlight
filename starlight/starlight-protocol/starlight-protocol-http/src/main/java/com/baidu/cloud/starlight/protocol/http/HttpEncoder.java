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
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.protocol.http.springrest.NettyServletResponseAdaptor;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultFullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpRequestEncoder;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseEncoder;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Encode {@link MsgBase} to {@link ByteBuf}, use
 * {@link com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObjectEncoder}. Encode msgBase body: from Pojo to
 * bytes. Created by liuruisen on 2020/5/27.
 */
public abstract class HttpEncoder implements ProtocolEncoder {

    private static final Integer EMPTY_BODY_LENGTH = 0;

    @Override
    public ByteBuf encode(MsgBase input) throws CodecException {
        if (input == null) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION, "MsgBase is null, cannot encode");
        }

        if (input instanceof Request) {
            return encodeRequest((Request) input);
        }

        if (input instanceof Response) {
            return encodeResponse((Response) input);
        }

        throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
            "MsgBase type is illegal {" + input.getClass().getName() + "}, cannot encode");

    }

    /**
     * Encode Request to ByteBuf
     *
     * @param request
     * @return
     */
    private ByteBuf encodeRequest(Request request) {

        if (request.getParams() == null || !(request.getParams()[0] instanceof FullHttpRequest)) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "RpcRequest have not been converted to HttpRequest, please check");
        }

        /**
         * {@link #encodeRequestBody(Request)} will convert RpcRequest to FullHttpRequest
         */
        FullHttpRequest httpRequest = (FullHttpRequest) request.getParams()[0];

        /**
         * Use EmbeddedChannel and HttpRequestEncoder to encode FullHttpRequest NOTICE:
         * HttpResponseEncoder的结果是直接分配在DirectBuf中的，使用完毕记得release
         */
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
        try {
            // FIXME 可根据实际情况调整channel的配置，主要是关于alloc().buffer()的配置
            embeddedChannel.writeOutbound(httpRequest);
            if (embeddedChannel.outboundMessages() != null && embeddedChannel.outboundMessages().size() > 1) {
                ByteBuf[] outputBufs = new ByteBuf[embeddedChannel.outboundMessages().size()];
                embeddedChannel.outboundMessages().toArray(outputBufs);
                return Unpooled.wrappedBuffer(outputBufs);
            }
            return embeddedChannel.readOutbound();
        } catch (Exception e) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "Encode Request to ByteBuf failed: " + e.getMessage());
        } finally {
            embeddedChannel.close();
        }
    }

    /**
     * Encode Response to ByteBuf
     *
     * @param response
     * @return
     */
    protected ByteBuf encodeResponse(Response response) {
        if (response.getResult() == null || !(response.getResult() instanceof FullHttpResponse)) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "RpcResponse have not been converted to HttpResponse, please check");
        }

        FullHttpResponse httpResponse = (FullHttpResponse) response.getResult();

        // NOTICE: HttpResponseEncoder的结果是直接分配在DirectBuf中的，使用完毕记得release
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpResponseEncoder());
        try {
            embeddedChannel.writeOutbound(httpResponse);
            if (embeddedChannel.outboundMessages() != null && embeddedChannel.outboundMessages().size() > 1) {
                ByteBuf[] outputBufs = new ByteBuf[embeddedChannel.outboundMessages().size()];
                embeddedChannel.outboundMessages().toArray(outputBufs);
                return Unpooled.wrappedBuffer(outputBufs);
            }
            return embeddedChannel.readOutbound();
        } catch (Exception e) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "Encode Response to ByteBuf failed: " + e.getMessage());
        } finally {
            embeddedChannel.close();
        }
    }

    /**
     * Convert MsgBase to {@link com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpMessage}
     *
     * @param msgBase
     * @throws CodecException
     */
    @Override
    public void encodeBody(MsgBase msgBase) throws CodecException {
        if (msgBase == null) {
            throw new CodecException(CodecException.BODY_ENCODE_EXCEPTION, "MsgBase is null, cannot encode");
        }

        if (msgBase instanceof Request) {
            encodeRequestBody((Request) msgBase);
        }

        if (msgBase instanceof Response) {
            encodeResponseBody((Response) msgBase);
        }
    }

    private void encodeRequestBody(Request request) {
        FullHttpRequest httpRequest = convertRequest(request);
        /**
         * EncodeRequestBody only serialize body an encode Response to FullHttpResponse. So we change params to
         * FullHttpRequest. {@link #encode(MsgBase)} method will get params from Request.params and convert to Request
         * byteBuf.
         */
        request.setParams(new Object[] {httpRequest});
    }

    protected void encodeResponseBody(Response response) {
        FullHttpResponse httpResponse = convertResponse(response);
        /**
         * EncodeResponseBody only serialize body an encode Response to FullHttpResponse. So we fill response result
         * with FullHttpResponse. {@link #encode(MsgBase)} method will get body from reponse.result and convert to
         * Response byteBuf.
         */
        response.setResult(httpResponse);
    }

    /**
     * Convert {@link RpcRequest} to {@link FullHttpRequest} in accordance with protocol and programming contract.
     *
     * @param rpcRequest
     * @return
     */
    protected abstract FullHttpRequest convertRequest(Request rpcRequest);

    /**
     * Convert {@link RpcResponse} to {@link FullHttpResponse} in accordance with protocol and programming contract.
     *
     * @param response
     * @return
     */
    protected FullHttpResponse convertResponse(Response response) {

        // <1> convert Response to FullHttpResponse
        FullHttpResponse httpResponse = null;

        if (hasHttpServletResponse(response)) { // 支持servlet请求的场景
            NettyServletResponseAdaptor servletResponseAdaptor = servletResponseAdaptor(response);
            httpResponse = servletResponseAdaptor.getNettyHttpResponse();
        } else {
            httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(AbstractHttpProtocol.UNKNOW_STATUS));
        }

        fulfillRpcResponse(httpResponse, response);
        // necessary headers
        httpResponse.headers().add(AbstractHttpProtocol.X_STARLIGHT_ID, response.getId()); // id
        if (response.getAttachmentKv() != null && response.getAttachmentKv().size() > 0) {
            for (Map.Entry<String, Object> kv : response.getAttachmentKv().entrySet()) {
                httpResponse.headers().add(kv.getKey(), kv.getValue());
            }
        }

        return httpResponse;
    }

    private boolean hasHttpServletResponse(Response response) {
        if (response.getRequest() == null) {
            return false;
        }

        // 兼容 servlet请求，即方法参数中的有HttpServletRequest HttpServletResponse
        Class<?>[] paramTypes = response.getRequest().getParamsTypes();
        if (paramTypes != null) {
            for (Class<?> paramClass : paramTypes) {
                if (HttpServletResponse.class.isAssignableFrom(paramClass)) {
                    return true;
                }
            }
        }

        Map<String, Object> requestAttachKv = response.getRequest().getAttachmentKv();
        if (requestAttachKv != null && requestAttachKv.get(Constants.SERVLET_RESPONSE_KEY) != null) {
            return true;
        }

        return false;
    }

    private NettyServletResponseAdaptor servletResponseAdaptor(Response response) {

        Object[] params = response.getRequest().getParams();
        if (params != null && params.length > 0) {
            for (Object param : params) {
                if (param instanceof NettyServletResponseAdaptor) {
                    return (NettyServletResponseAdaptor) param;
                }
            }
        }

        Map<String, Object> requestAttachKv = response.getRequest().getAttachmentKv();
        if (requestAttachKv != null && requestAttachKv.get(Constants.SERVLET_RESPONSE_KEY) != null) {
            return (NettyServletResponseAdaptor) requestAttachKv.remove(Constants.SERVLET_RESPONSE_KEY);
        }

        return null;
    }

    private Serializer serializer(String protocolName) {
        Protocol httpProtocol = ExtensionLoader.getInstance(Protocol.class).getExtension(protocolName);
        Serializer serializer = httpProtocol.getSerialize();
        return serializer;
    }

    protected void fulfillRpcResponse(FullHttpResponse httpResponse, Response response) {
        Serializer serializer = serializer(response.getProtocolName());

        // set response status
        if (httpResponse.status() == null || httpResponse.status().code() == AbstractHttpProtocol.UNKNOW_STATUS) {
            if (response.getStatus() != Constants.SUCCESS_CODE) {
                httpResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            } else {
                httpResponse.setStatus(HttpResponseStatus.OK);
            }
        }

        // set content
        if (!httpResponse.status().equals(HttpResponseStatus.OK) && StringUtils.hasText(response.getErrorMsg())) {
            response.setBodyBytes(serializer.serialize(response.getErrorMsg(), String.class));
        }

        if (httpResponse.status().equals(HttpResponseStatus.OK) && response.getResult() != null) {
            if (AbstractHttpProtocol.isServletRequest(response.getRequest())) { // servlet request
                if (response.getResult().getClass() == byte[].class) { // 返回byte[]的类型
                    response.setBodyBytes((byte[]) response.getResult());
                } else if (response.getResult() instanceof String) { // 返回 string 类型
                    response.setBodyBytes(((String) response.getResult()).getBytes());
                } else { // 其他类型
                    response.setBodyBytes(serializer.serialize(response.getResult(), response.getGenericReturnType()));
                }
            } else {
                response.setBodyBytes(serializer.serialize(response.getResult(), response.getGenericReturnType()));
            }
        }
        // httpResponse.content().clear();
        if (response.getBodyBytes() != null && response.getBodyBytes().length > 0) {
            httpResponse.content().writeBytes(response.getBodyBytes());
        }
        int contentLength = httpResponse.content().readableBytes();
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
    }
}
