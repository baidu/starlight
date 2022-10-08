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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.transport.channel.ThreadLocalChannelContext;
import com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol;
import com.baidu.cloud.starlight.protocol.http.HttpDecoder;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultFullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.springframework.http.HttpHeaders;
import com.baidu.cloud.thirdparty.springframework.http.converter.HttpMessageConversionException;
import com.baidu.cloud.thirdparty.springframework.web.method.HandlerMethod;

import com.baidu.cloud.thirdparty.servlet.ServletException;
import com.baidu.cloud.thirdparty.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruisen on 2020/6/5.
 */
public class SpringRestHttpDecoder extends HttpDecoder {

    /**
     * Convert httpRequest to Request:
     * <ul>
     *     <li>step1: convert headers to kvMap, requestId</li>
     *     <li>step2: Get HandlerMethod map and resolve args</li>
     * </ul>
     */
    @Override
    protected Request reverseConvertRequest(FullHttpRequest httpRequest) {
        long corelationId = Long.parseLong(httpRequest.headers().get(SpringRestProtocol.X_STARLIGHT_ID));
        Request request = new RpcRequest(corelationId);
        request.setProtocolName(SpringRestProtocol.PROTOCOL_NAME);
        request.setAttachmentKv(new HashMap<>());
        // store all request Headers to Request kv attachment
        for (Map.Entry<String, String> entry : httpRequest.headers().entries()) {
            request.getAttachmentKv().put(entry.getKey(), entry.getValue());
        }

        Channel channel = ThreadLocalChannelContext.getContext().getChannel();
        // convert FullHttpRequest to HttpServletRequest
        NettyServletRequestAdaptor servletRequestAdaptor = new NettyServletRequestAdaptor(httpRequest, channel);

        // A response of the intermediate state, which will be replaced in the subsequent process
        NettyServletResponseAdaptor servletResponseAdaptor =
            new NettyServletResponseAdaptor(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(AbstractHttpProtocol.UNKNOW_STATUS)), servletRequestAdaptor);

        // put servlet request and response to attachment, these will be put into RpcContext
        // remember to remove when return response
        // support servlet request
        // 实现类似从Context中获取servlet的内容
        request.getAttachmentKv().put(Constants.SERVLET_REQUEST_KEY, servletRequestAdaptor);
        request.getAttachmentKv().put(Constants.SERVLET_RESPONSE_KEY, servletResponseAdaptor);

        try {
            // 请求映射
            SpringRestHandlerMapping handlerMapping = SpringRestHandlerMapping.getInstance();
            HandlerMethod handlerMethod = handlerMapping.handler(servletRequestAdaptor);
            if (handlerMethod == null) {
                throw new NoHandlerFoundException(servletRequestAdaptor.getMethod(),
                    servletRequestAdaptor.getRequestURI(), new HttpHeaders());
            }

            Class<?> serviceClass = handlerMethod.getBeanType();
            if (serviceClass.getInterfaces().length > 0) {
                serviceClass = serviceClass.getInterfaces()[0];
            }
            request.setServiceName(serviceClass.getName()); // set serviceName
            request.setMethodName(handlerMethod.getMethod().getName()); // set methodName
            request.setParamsTypes(handlerMethod.getMethod().getParameterTypes()); // set paramsTypes
            request.setGenericParamsTypes(handlerMethod.getMethod().getGenericParameterTypes());
            request.setMethod(handlerMethod.getMethod());
            // set service obj so we can use it without ServiceRegistry#discover
            request.setServiceObj(handlerMethod.getBean());

            // resolve param value:
            // <1> if the param type is HttpServletRequest or HttpServletResponse,
            // the value of it is NettyServletRequestAdapter or NettyServletResponseAdapter instance.
            // @see ServletRequestMethodArgumentResolver ServletResponseMethodArgumentResolver
            // <2> if the param type is others, the value of it is real object
            // <3> if the param type is others and it behaves as @RequestBody,
            // the param value will remain in byte form and wait for subsequent deserialization
            // @see BytesHttpMessageConverter
            Object[] args =
                handlerMapping.resolveArguments(handlerMethod, servletRequestAdaptor, servletResponseAdaptor);

            request.setParams(args); // set params

            return request;
        } catch (Exception e) {
            if (e instanceof ServletException || e instanceof HttpMessageConversionException) {
                responseNotSupport(request, servletResponseAdaptor, channel, e.getMessage());
                // TODO 直接响应异常信息给客户端，思路是采取异常对象中封装response的方式
                throw new CodecException(CodecException.PROTOCOL_DECODE_EXCEPTION,
                    "Error occur when use SpringRestHttpDecoder to reverseConvertRequest: " + e.getMessage());
            }
            if (e instanceof CodecException) {
                throw (CodecException) e;
            }
            // if throw exception in convert procedure, we think can't use this protocol to decode(not match)
            throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                "Cannot not use SpringRestProtocol to decode request bytes");
        } finally {
            // FIXME 多协议支持可能要移动位置
            if (httpRequest.refCnt() > 0) {
                LOGGER.debug("Release httpRequest refCnt {}", httpRequest.refCnt());
                httpRequest.release();
            }
        }
    }

    /**
     * 对于找不到handler映射、请求uri不支持的情况，直接响应不支持异常
     * 
     * @param request
     * @param servletResponseAdaptor
     * @param channel
     * @param errorMsg
     */
    private void responseNotSupport(Request request, NettyServletResponseAdaptor servletResponseAdaptor,
        Channel channel, String errorMsg) {
        // when handlerMethod is null, it means there is no handler in SpringRestProtocol,
        // so we think decode error.
        // FIXME Maybe it can be decoded with another HttpProtocol.
        // FIXME Currently set to decode failure first,
        // FIXME when multiple http protocols are implemented, please check if the logic here is as expected

        Response response = new RpcResponse(request.getId());
        response.setProtocolName(SpringRestProtocol.PROTOCOL_NAME);
        response.setStatus(CodecException.PROTOCOL_DECODE_EXCEPTION);
        response.setErrorMsg(errorMsg);
        response.setRequest(request);

        try {
            servletResponseAdaptor.setStatus(HttpResponseStatus.NOT_FOUND.code());

            SpringRestHttpEncoder encoder = new SpringRestHttpEncoder();
            encoder.encodeBody(response);

            channel.writeAndFlush(response);
        } catch (CodecException e) {
            LOGGER.error("Error occur when return uri not support error to request, cause by {}", e.getMessage());
        }
    }

    @Override
    protected Response reverseConvertResponse(FullHttpResponse httpResponse) {
        Response response = super.reverseConvertResponse(httpResponse);
        if (response != null) {
            response.setProtocolName(SpringRestProtocol.PROTOCOL_NAME);
        }
        return response;
    }
}
