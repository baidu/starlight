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
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.protocol.http.HttpEncoder;
import com.baidu.cloud.thirdparty.apache.commons.lang3.StringUtils;
import com.baidu.cloud.thirdparty.feign.Feign;
import com.baidu.cloud.thirdparty.feign.MethodMetadata;
import com.baidu.cloud.thirdparty.feign.RequestTemplate;
import com.baidu.cloud.thirdparty.feign.Target;
import com.baidu.cloud.thirdparty.feign.querymap.FieldQueryMapEncoder;
import com.baidu.cloud.thirdparty.feign.spring.SpringContract;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultFullHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpMessage;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.FullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderValues;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpMethod;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.springframework.http.MediaType;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.ModelAttribute;
import com.baidu.cloud.thirdparty.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import com.baidu.cloud.thirdparty.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * HttpDecoder that can encode {@link RpcRequest} to {@link FullHttpRequest} in accordance with spring-mvc annotation.
 * Created by liuruisen on 2020/6/4.
 */
public class SpringRestHttpEncoder extends HttpEncoder {

    private static final SpringContract contract = new SpringContract();

    private final Map<String, RequestTemplateGenerator> requestTemplateGeneratorCache;

    private final Map<Class<?>, String> parsedServiceClass;

    public SpringRestHttpEncoder() {
        this.requestTemplateGeneratorCache = new ConcurrentHashMap<>();
        this.parsedServiceClass = new ConcurrentHashMap<>();
    }

    /**
     * Parse spring-mvc rest annotations. Convert {@link RpcRequest} to {@link FullHttpMessage}. Serialize body if has.
     *
     * @param rpcRequest
     * @return
     */
    @Override
    protected FullHttpRequest convertRequest(Request rpcRequest) {
        if (GenericUtil.isGenericCall(rpcRequest)) {
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "SpringRest protocol not support generic call");
        }

        Class<?> serviceClass = rpcRequest.getServiceClass();
        if (parsedServiceClass.get(serviceClass) == null) { // new service, parse and validate meta
            parseAndValidateMeta(serviceClass);
        }

        Method targetMethod = rpcRequest.getMethod();
        RequestTemplateGenerator generator = // Feign.configKey generate unique key for target method
            requestTemplateGeneratorCache.get(Feign.configKey(serviceClass, targetMethod));
        if (generator == null) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "Encode request with SpringRestProtocol failed: can not find parsed methodMeta, " + "maybe the method {"
                    + targetMethod.getName() + "} is illegal");
        }

        RequestTemplate template = generator.create(rpcRequest.getParams());
        return convert(template, rpcRequest);
    }

    // TODO move to init logic 可能会有性能影响，考虑在初始化阶段进行解析存储

    /**
     * Parse and validate metadata(Annotations) on serviceClass.
     *
     * @param serviceClass
     */
    private void parseAndValidateMeta(Class<?> serviceClass) {
        List<MethodMetadata> methodMetadatas = contract.parseAndValidateMetadata(serviceClass);
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(SpringRestProtocol.PROTOCOL_NAME);
        for (MethodMetadata metadata : methodMetadatas) {
            RequestTemplateGenerator generator = null;
            if (metadata.bodyIndex() != null) {
                if (isFormData(metadata)) {
                    generator = new RequestTemplateGenerator(metadata, new FormRequestTemplateArgsResolver(
                        new FieldQueryMapEncoder(), Target.EmptyTarget.create(serviceClass)));
                } else {
                    generator = new RequestTemplateGenerator(metadata, new EncodedRequestTemplateArgsResolver(
                        new FieldQueryMapEncoder(), Target.EmptyTarget.create(serviceClass), protocol.getSerialize()));
                }

            } else {
                generator =
                    new RequestTemplateGenerator(metadata, new RequestTemplateArgsResolver(new FieldQueryMapEncoder(),
                        Target.EmptyTarget.create(serviceClass)));
            }

            requestTemplateGeneratorCache.put(metadata.configKey(), generator);
        }

        parsedServiceClass.put(serviceClass, serviceClass.getSimpleName());
    }

    /**
     * Use {@link RequestTemplate} to convert {@link Request} to {@link FullHttpRequest}.
     *
     * @param requestTemplate
     * @return
     */
    private FullHttpRequest convert(RequestTemplate requestTemplate, Request request) {
        com.baidu.cloud.thirdparty.feign.Request feignRequest = requestTemplate.request();

        FullHttpRequest httpRequest = null;
        if (feignRequest.body() != null && feignRequest.body().length > 0) {
            httpRequest =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(feignRequest.httpMethod().name()),
                    feignRequest.url(), Unpooled.wrappedBuffer(feignRequest.body()));
            httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE,
                SpringRestProtocol.CONTENT_TYPE_JSON + ";" + SpringRestProtocol.ENCODING);
            httpRequest.headers().add(HttpHeaderNames.CONTENT_LENGTH, feignRequest.body().length);
        } else {
            httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(feignRequest.httpMethod().name()), feignRequest.url());
        }

        // necessary header
        httpRequest.headers().add(SpringRestProtocol.X_STARLIGHT_ID, request.getId());
        httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        FullHttpRequest finalHttpRequest = httpRequest;
        Function<String, Boolean> findRepeatHeaderFunc = (newHeaderKey) ->
                finalHttpRequest.headers()
                        .names()
                        .stream()
                        .filter(name -> StringUtils.equalsAnyIgnoreCase(name, newHeaderKey))
                        .findAny()
                        .isPresent();

        // request kvAttachment
        if (request.getAttachmentKv() != null) {
            for (Map.Entry<String, Object> kv : request.getAttachmentKv().entrySet()) {
                if (kv.getKey() != null && kv.getValue() != null && !findRepeatHeaderFunc.apply(kv.getKey())) {
                    httpRequest.headers().add(kv.getKey(), kv.getValue());
                }
            }
        }

        // TODO 调研Netty对一个Header多个值的支持
        // feignRequest headers
        if (feignRequest.headers().size() > 0) {
            for (Map.Entry<String, Collection<String>> header : feignRequest.headers().entrySet()) {
                if (!findRepeatHeaderFunc.apply(header.getKey())) {
                    httpRequest.headers().add(header.getKey(), header.getValue());
                }
            }
        }

        return httpRequest;
    }

    @Override
    protected FullHttpResponse convertResponse(Response response) {
        return super.convertResponse(response);
    }

    @Override
    protected void fulfillRpcResponse(FullHttpResponse httpResponse, Response response) {

        // clear
        NettyServletRequestAdaptor servletRequestAdaptor =
            (NettyServletRequestAdaptor) response.getRequest().getAttachmentKv().remove(Constants.SERVLET_REQUEST_KEY);

        // set content-type
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType(servletRequestAdaptor, response));
        super.fulfillRpcResponse(httpResponse, response);
    }

    private String contentType(NettyServletRequestAdaptor servletRequestAdaptor, Response response) {
        SpringRestHandlerMapping handlerMapping = SpringRestHandlerMapping.getInstance();

        // maybe decode error, such as not request uri is not found
        if (response.getRequest().getMethod() == null) {
            return HttpHeaderValues.APPLICATION_JSON.toString();
        }

        RequestMappingInfo requestMappingInfo = handlerMapping.getMappingForMethod(response.getRequest().getMethod(),
            response.getRequest().getMethod().getDeclaringClass());
        // default value
        if (requestMappingInfo == null) {
            return HttpHeaderValues.APPLICATION_JSON.toString();
        }

        ProducesRequestCondition producesRequestCondition =
            requestMappingInfo.getProducesCondition().getMatchingCondition(servletRequestAdaptor);

        if (producesRequestCondition != null && producesRequestCondition.getExpressions().size() > 0) {
            MediaType contentMediaType = producesRequestCondition.getExpressions().iterator().next().getMediaType();
            return contentMediaType.toString();
        } else { // default is application/json
            return HttpHeaderValues.APPLICATION_JSON.toString();
        }
    }

    private boolean isFormData(MethodMetadata metadata) {

        Annotation[][] parameterAnnotations = metadata.method().getParameterAnnotations();
        if (parameterAnnotations != null) {
            for (Annotation[] annotations : parameterAnnotations) {
                if (annotations == null) {
                    continue;
                }
                for (Annotation annotation : annotations) {
                    boolean match = ModelAttribute.class.isAssignableFrom(annotation.getClass());
                    if (match) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
