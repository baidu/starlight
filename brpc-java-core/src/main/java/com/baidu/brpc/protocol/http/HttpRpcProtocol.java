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

package com.baidu.brpc.protocol.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.baidu.brpc.protocol.standard.BaiduRpcProto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.naming.DnsNamingService;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.BrpcMeta;
import com.baidu.brpc.protocol.HttpRequest;
import com.baidu.brpc.protocol.HttpResponse;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.ServiceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * 处理http rpc协议，包括四种序列化格式：
 * 1、http + protobuf
 * 2、http + json
 */
public class HttpRpcProtocol extends AbstractProtocol {
    public static final String PROTOCOL_TYPE = "protocol-type";
    private static final Logger LOG = LoggerFactory.getLogger(HttpRpcProtocol.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_PROTOBUF = "application/proto";
    /**
     * 请求的唯一标识id
     */
    private static final String CORRELATION_ID = "correlation-id";
    private static final JsonFormat jsonPbConverter = new JsonFormat() {
        protected void print(Message message, JsonGenerator generator) throws IOException {
            for (Iterator<Map.Entry<Descriptors.FieldDescriptor, Object>> iter =
                 message.getAllFields().entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<Descriptors.FieldDescriptor, Object> field = iter.next();
                printField(field.getKey(), field.getValue(), generator);
                if (iter.hasNext()) {
                    generator.print(",");
                }
            }
            // ignore UnknownFields
        }
    };
    private static final Gson gson = (new GsonBuilder())
            .serializeNulls()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .create();
    private static final JsonParser jsonParser = new JsonParser();

    // HTTP Headers which should not be modified by user
    private static final Set<String> prohibitedHeaders = new HashSet<String>();

    static {
        prohibitedHeaders.add(HttpHeaderNames.CONTENT_TYPE.toString());
        prohibitedHeaders.add(HttpHeaderNames.CONTENT_LENGTH.toString());
        prohibitedHeaders.add(HttpHeaderNames.CONNECTION.toString());
        prohibitedHeaders.add(CORRELATION_ID);
    }


    protected int protocolType;
    protected String encoding;

    public HttpRpcProtocol(int protocolType, String encoding) {
        this.protocolType = protocolType;
        this.encoding = encoding;
    }

    @Override
    public Request createRequest() {
        return new HttpRequest();
    }

    @Override
    public Response createResponse() {
        return new HttpResponse();
    }

    @Override
    public Request getRequest() {
        Request request = HttpRequest.getHttpRequest();
        request.reset();
        return request;
    }

    @Override
    public Response getResponse() {
        Response response = HttpResponse.getHttpResponse();
        response.reset();
        return response;
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        HttpMessage httpMessage = null;
        // I don't know the length of http header, so here copy all readable bytes to decode
        ByteBuf byteBuf = in.retainedSlice(in.readableBytes());
        boolean decodeSuccess = false;
        try {
            // TODO: only parse header
            httpMessage = (HttpMessage) BrpcHttpObjectDecoder.getDecoder(isDecodingRequest).decode(ctx, byteBuf);
            if (httpMessage != null) {
                if (httpMessage.decoderResult() != null && httpMessage.decoderResult().isFailure()) {
                    // could not decode http message
                    LOG.debug("failed to decode http message", httpMessage.decoderResult().cause());
                    throw new BadSchemaException();
                }
                String contentTypeAndEncoding = httpMessage.headers().get(HttpHeaderNames.CONTENT_TYPE);
                // if content-type does not exist, it is /status request, so this protocol can deal with.
                if (StringUtils.isNoneBlank(contentTypeAndEncoding)) {
                    contentTypeAndEncoding = contentTypeAndEncoding.toLowerCase();
                    String[] splits = StringUtils.split(contentTypeAndEncoding, ";");
                    String contentType = splits[0];
                    if (!contentType.equals(CONTENT_TYPE_PROTOBUF)
                            && !contentType.equals(CONTENT_TYPE_JSON)) {
                        // this protocol can only deal with http protobuf and http json request.
                        httpMessage = null;
                        throw new BadSchemaException();
                    }
                }
                decodeSuccess = true;
            }
        } catch (Exception e) {
            throw new BadSchemaException();
        } finally {
            if (decodeSuccess) {
                in.skipBytes(byteBuf.readerIndex());
            }
            byteBuf.release();
        }

        if (httpMessage == null) {
            // decode next time
            throw notEnoughDataException;
        }

        return httpMessage;
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        HttpRequest httpRequest = (HttpRequest) request;
        String serviceName = httpRequest.getTargetMethod().getDeclaringClass().getName();
        String methodName = httpRequest.getTargetMethod().getName();
        BrpcMeta rpcMeta = httpRequest.getTargetMethod().getAnnotation(BrpcMeta.class);
        if (rpcMeta != null) {
            serviceName = rpcMeta.serviceName();
            methodName = rpcMeta.methodName();
        }
        LOG.debug("serviceName={}, methodName={}", serviceName, methodName);

        Object httpRequestBody = makeRequest((int) httpRequest.getLogId(), methodName, httpRequest.getArgs());
        byte[] httpRequestBodyBytes =
                encodeBody(protocolType, encoding, httpRequestBody, httpRequest.getRpcMethodInfo());

        FullHttpRequest nettyHttpRequest = null;
        try {
            nettyHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "");
            nettyHttpRequest.setUri(buildHttpUri(serviceName, methodName));
            if (httpRequestBodyBytes != null) {
                nettyHttpRequest.content().writeBytes(httpRequestBodyBytes);
            }
            String contentType = getContentType(protocolType);
            nettyHttpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=" + encoding);
            nettyHttpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpRequestBodyBytes == null
                    ? 0 : httpRequestBodyBytes.length);
            nettyHttpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            nettyHttpRequest.headers().set(CORRELATION_ID, httpRequest.getCorrelationId());
            for (Map.Entry<String, String> header : httpRequest.headers()) {
                if (prohibitedHeaders.contains(header.getKey().toLowerCase())) {
                    continue;
                }
                nettyHttpRequest.headers().set(header.getKey(), header.getValue());
            }
            if (request.getKvAttachment() != null) {
                for (Map.Entry<String, Object> kv : request.getKvAttachment().entrySet()) {
                    if (!prohibitedHeaders.contains(kv.getKey().toLowerCase())) {
                        nettyHttpRequest.headers().set(kv.getKey(), kv.getValue());
                    }
                }
            }
            BrpcHttpRequestEncoder encoder = new BrpcHttpRequestEncoder();
            return encoder.encode(nettyHttpRequest);
        } finally {
            if (nettyHttpRequest != null) {
                nettyHttpRequest.release();
            }
        }
    }

    @Override
    public void beforeRequestSent(Request request, RpcClient rpcClient, BrpcChannel channelGroup) {
        String hostPort;
        HttpRequest httpRequest = (HttpRequest) request;
        NamingService namingService = rpcClient.getNamingService();
        if (!httpRequest.headers().contains(HttpHeaderNames.HOST)) {
            if (namingService != null && namingService instanceof DnsNamingService) {
                // 从 DnsNamingService 获取原始的 host
                hostPort = ((DnsNamingService) namingService).getHostPort();
            } else {
                // 默认获取当前链接的 host:port 即可
                hostPort = channelGroup.getServiceInstance().getIp() + ":"
                        + channelGroup.getServiceInstance().getPort();
            }
            // some http server decide what to do by the 'host' param in request header
            httpRequest.headers().set(HttpHeaderNames.HOST, hostPort);
        }
    }

    @Override
    public Response decodeResponse(Object msg, ChannelHandlerContext ctx) {
        FullHttpResponse httpResponse = (FullHttpResponse) msg;
        try {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            Long correlationId = parseCorrelationId(httpResponse.headers().get(CORRELATION_ID), channelInfo.getCorrelationId());
            HttpResponse response = new HttpResponse();
            response.setCorrelationId(correlationId);
            RpcFuture future = channelInfo.removeRpcFuture(response.getCorrelationId());
            if (future == null) {
                return response;
            }
            response.setRpcFuture(future);

            int bodyLen = httpResponse.content().readableBytes();
            byte[] bytes = new byte[bodyLen];
            httpResponse.content().readBytes(bytes);

            if (!httpResponse.status().equals(HttpResponseStatus.OK)) {
                String body = new String(bytes);
                String message = String.format("http status=%d, message=%s",
                        httpResponse.status().code(), body);
                LOG.warn("{}", message);
                response.setException(new RpcException(RpcException.SERVICE_EXCEPTION, message));
                return response;
            }

            String contentTypeAndEncoding = httpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE).toLowerCase();
            String[] splits = StringUtils.split(contentTypeAndEncoding, ";");
            int protocolType = HttpRpcProtocol.parseProtocolType(splits[0]);
            String encoding = this.encoding;
            // 由于uc服务返回的encoding是错误的，所以这里以client端设置的encoding为准。
            //        for (String split : splits) {
            //            split = split.trim();
            //            if (split.startsWith("charset=")) {
            //                encoding = split.substring("charset=".length());
            //            }
            //        }

            Object body = null;
            if (bodyLen != 0) {
                try {
                    body = decodeBody(protocolType, encoding, bytes);
                } catch (Exception ex) {
                    LOG.error("decode response body failed");
                    response.setException(ex);
                    return response;
                }
            }

            if (body != null) {
                try {
                    response.setResult(parseHttpResponse(body, future.getRpcMethodInfo()));
                } catch (Exception ex) {
                    LOG.error("failed to parse result from HTTP body");
                    response.setException(ex);
                }
            } else {
                response.setResult(null);
            }

            // set response attachment
            if (response.getKvAttachment() == null) {
                response.setKvAttachment(new HashMap<String, Object>());
            }
            for (Map.Entry<String, String> entry : httpResponse.headers()) {
                response.getKvAttachment().put(entry.getKey(), entry.getValue());
            }

            return response;
        } finally {
            httpResponse.release();
        }
    }

    @Override
    public Request decodeRequest(Object packet) {
        try {
            HttpRequest httpRequest = (HttpRequest) this.createRequest();
            httpRequest.setMsg(packet);
            long correlationId = parseCorrelationId(httpRequest.headers().get(CORRELATION_ID), null);
            httpRequest.setCorrelationId(correlationId);

            String contentTypeAndEncoding = httpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE).toLowerCase();
            String[] splits = StringUtils.split(contentTypeAndEncoding, ";");
            int protocolType = HttpRpcProtocol.parseProtocolType(splits[0]);
            String encoding = this.encoding;
            for (String split : splits) {
                split = split.trim();
                if (split.startsWith("charset=")) {
                    encoding = split.substring("charset=".length());
                }
            }
            httpRequest.headers().set(PROTOCOL_TYPE, protocolType);
            httpRequest.headers().set(HttpHeaderNames.CONTENT_ENCODING, encoding);

            // set http headers to attachment
            if (httpRequest.getKvAttachment() == null) {
                httpRequest.setKvAttachment(new HashMap<String, Object>());
            }
            for (Map.Entry<String, String> entry : httpRequest.headers()) {
                httpRequest.getKvAttachment().put(entry.getKey(), entry.getValue());
            }

            ByteBuf byteBuf = httpRequest.content();
            int bodyLen = byteBuf.readableBytes();
            if (bodyLen == 0) {
                String errMsg = String.format("body should not be null, uri:%s", httpRequest.uri());
                LOG.warn(errMsg);
                httpRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
                return httpRequest;
            }
            byte[] requestBytes = new byte[bodyLen];
            byteBuf.readBytes(requestBytes, 0, bodyLen);
            Object body = decodeBody(protocolType, encoding, requestBytes);

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
            String path = queryStringDecoder.path();
            String serviceName = null;
            String methodName = null;
            if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE
                    || protocolType == Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE) {
                String[] uriSplit = path.split("/");
                if (uriSplit.length < 3) {
                    String errMsg = String.format("url format is error, path:%s", path);
                    LOG.warn(errMsg);
                    httpRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
                    return httpRequest;
                }
                serviceName = uriSplit[uriSplit.length - 2];
                methodName = uriSplit[uriSplit.length - 1];
            } else {
                JsonObject bodyObject = (JsonObject) body;
                methodName = bodyObject.get("method").getAsString();
                serviceName = path;
            }
            ServiceManager serviceManager = ServiceManager.getInstance();
            RpcMethodInfo rpcMethodInfo = serviceManager.getService(serviceName, methodName);
            if (rpcMethodInfo == null) {
                String errMsg = String.format("Fail to find path=%s", path);
                LOG.warn(errMsg);
                httpRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
                return httpRequest;
            }
            httpRequest.setServiceName(rpcMethodInfo.getServiceName());
            httpRequest.setMethodName(rpcMethodInfo.getMethodName());
            httpRequest.setRpcMethodInfo(rpcMethodInfo);
            httpRequest.setTargetMethod(rpcMethodInfo.getMethod());
            httpRequest.setTarget(rpcMethodInfo.getTarget());
            httpRequest.setArgs(parseRequestParam(protocolType, body, rpcMethodInfo));
            return httpRequest;
        } finally {
            ((FullHttpRequest) packet).release();
        }
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) {
        FullHttpRequest httpRequest = (FullHttpRequest) request.getMsg();
        FullHttpResponse httpResponse = null;

        try {
            byte[] responseBytes;
            if (response.getException() != null) {
                httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                responseBytes = response.getException().toString().getBytes();
            } else {
                httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                int protocolType = Integer.parseInt(httpRequest.headers().get(PROTOCOL_TYPE));
                Object body = makeResponse(protocolType, response);
                // encode body
                try {
                    responseBytes = encodeBody(protocolType,
                            httpRequest.headers().get(HttpHeaderNames.CONTENT_ENCODING),
                            body, response.getRpcMethodInfo());
                } catch (Exception e) {
                    LOG.warn("encode response failed", e);
                    response.setException(e);
                    httpResponse = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    responseBytes = response.getException().toString().getBytes();
                }
            }
            httpResponse.content().writeBytes(responseBytes);
            addHttpResponseHeaders(httpResponse, response, httpRequest);
            // encode full http response
            BrpcHttpResponseEncoder encoder = new BrpcHttpResponseEncoder();
            return encoder.encode(httpResponse);
        } catch (Exception e) {
            LOG.warn("encode response failed", e);
            response.setException(e);
            return null;
        } finally {
            if (httpResponse != null) {
                httpResponse.release();
            }
        }
    }

    @Override
    public void afterResponseSent(Request request, Response response, ChannelFuture channelFuture) {
        if (!HttpUtil.isKeepAlive(((HttpRequest) request).getNettyHttpRequest())) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public byte[] encodeResponseBody(int protocolType, Request request, Response response) {
        FullHttpRequest httpRequest = (FullHttpRequest) request.getMsg();
        Object body = makeResponse(protocolType, response);
        return encodeBody(protocolType,
                httpRequest.headers().get(HttpHeaderNames.CONTENT_ENCODING),
                body, response.getRpcMethodInfo());
    }

    @Override
    public boolean returnChannelBeforeResponse() {
        return false;
    }

    @Override
    public boolean isCoexistence() {
        return true;
    }

    public static int parseProtocolType(String contentType) {
        String contentType2 = contentType.toLowerCase();
        if (contentType2.equals(HttpRpcProtocol.CONTENT_TYPE_JSON)) {
            return Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE;
        } else if (contentType2.equals(HttpRpcProtocol.CONTENT_TYPE_PROTOBUF)) {
            return Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE;
        } else {
            LOG.warn("unknown contentType={}", contentType);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "unknown contentType=" + contentType);
        }
    }

    public String getContentType(Integer protocolType) {
        String contentType;
        switch (protocolType) {
            case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE: {
                contentType = HttpRpcProtocol.CONTENT_TYPE_JSON;
                break;
            }
            case Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE: {
                contentType = HttpRpcProtocol.CONTENT_TYPE_PROTOBUF;
                break;
            }
            default:
                LOG.warn("unknown protocolType={}", protocolType);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                        "unknown protocolType=" + protocolType);
        }
        return contentType;
    }

    public byte[] encodeBody(int protocolType, String encoding, Object body, RpcMethodInfo rpcMethodInfo) {
        byte[] bodyBytes;
        try {
            switch (protocolType) {
                case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE: {
                    String bodyJson = "";
                    if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        jsonPbConverter.print((Message) body, out, Charset.forName(encoding));
                        out.flush();
                        bodyJson = out.toString(encoding);
                    } else {
                        bodyJson = gson.toJson(body);
                    }
                    bodyBytes = bodyJson.getBytes(encoding);
                    break;
                }
                case Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE: {
                    if (rpcMethodInfo.getTarget() != null) {
                        // server端
                        bodyBytes = rpcMethodInfo.outputEncode(body);
                    } else {
                        bodyBytes = rpcMethodInfo.inputEncode(body);
                    }
                    break;
                }
                default:
                    LOG.warn("unkown protocolType={}", protocolType);
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                            "unkown protocolType=" + protocolType);
            }
        } catch (Exception ex) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "encode body failed", ex);
        }
        return bodyBytes;
    }

    public Object decodeBody(int protocolType, String encoding, byte[] bytes) {
        Object body = null;
        try {
            switch (protocolType) {
                case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE: {
                    body = new String(bytes, encoding);
                    break;
                }
                case Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE: {
                    // decode when parse response
                    body = bytes;
                    break;
                }
                default:
                    LOG.warn("unknown protocolType={}", protocolType);
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                            "unknown protocolType=" + protocolType);
            }
        } catch (Exception ex) {
            LOG.error("decodeBody failed", ex);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "decode body failed", ex);
        }
        return body;
    }

    /**
     * fill http response headers
     * @param fullHttpResponse netty http response
     * @param response brpc standard response
     * @param fullHttpRequest netty http request
     */
    public void addHttpResponseHeaders(FullHttpResponse fullHttpResponse,
                                       Response response,
                                       FullHttpRequest fullHttpRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        if (keepAlive) {
            fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE,
                fullHttpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE));
        if (fullHttpRequest.headers().contains("callId")) {
            fullHttpResponse.headers().set("callId", fullHttpRequest.headers().get("callId"));
        }
        if (fullHttpRequest.headers().contains(CORRELATION_ID)) {
            fullHttpResponse.headers().set(CORRELATION_ID, fullHttpRequest.headers().get(CORRELATION_ID));
        }
        if (response.getKvAttachment() != null) {
            for (Map.Entry<String, Object> entry : response.getKvAttachment().entrySet()) {
                fullHttpResponse.headers().set(entry.getKey(), entry.getValue());
            }
        }
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, fullHttpResponse.content().readableBytes());
    }

    public Object makeRequest(int id, String methodName, Object[] args) {
        if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE) {
            if (args == null || args.length == 0) {
                return null;
            }
            return args[0];
        } else if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE) {
            if (args == null || args.length == 0) {
                return null;
            }
            return args[0];

        } else {
            Map<String, Object> map = new HashMap();
            map.put("jsonrpc", "2.0");
            map.put("method", methodName);
            if (args != null) {
                map.put("params", args);
            } else {
                map.put("params", new Object[0]);
            }

            map.put("id", "" + id);
            return gson.toJsonTree(map);
        }
    }

    public Object makeResponse(int protocolType, Response response) {
        Long id = response.getLogId();
        if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE) {
            return response.getResult();
        } else if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE) {
            return response.getResult();
        } else {
            JsonObject res = new JsonObject();
            JsonElement result = gson.toJsonTree(
                    response.getResult(), response.getRpcMethodInfo().getMethod().getReturnType());
            res.addProperty("jsonrpc", "2.0");
            if (result != null) {
                res.add("result", result);
            } else {
                res.addProperty("error", "bad request");
            }
            if (id != null) {
                res.addProperty("id", id.intValue());
            }

            return res;
        }
    }

    public String buildHttpUri(String serviceName, String methodName) {
        // uri格式为 /serviceName/methodName
        return "/" + serviceName + "/" + methodName;
    }

    // 解析correlationId
    public long parseCorrelationId(String headerCorrelationId, Long channelAttachCorrelationId) {
        // 以headerCorrelationId为准，headerCorrelationId为null则以channelAttachCorrelationId为准
        if (headerCorrelationId != null) {
            return Long.valueOf(headerCorrelationId);
        } else if (channelAttachCorrelationId != null) {
            return channelAttachCorrelationId;
        }
        return -1;
    }

    public Object parseHttpResponse(Object body, RpcMethodInfo rpcMethodInfo) {
        Object response;
        try {
            switch (protocolType) {
                case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE:
                    if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                        ProtobufRpcMethodInfo protobufRpcMethodInfo = (ProtobufRpcMethodInfo) rpcMethodInfo;
                        Message.Builder rspBuilder = protobufRpcMethodInfo.getOutputInstance().newBuilderForType();
                        jsonPbConverter.merge((String) body, ExtensionRegistry.getEmptyRegistry(), rspBuilder);
                        response = rspBuilder.build();
                    } else {
                        response = gson.fromJson((String) body, rpcMethodInfo.getOutputClass());
                    }
                    break;
                case Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE:
                    response = rpcMethodInfo.outputDecode((byte[]) body);
                    break;
                default:
                    LOG.warn("unknown protocolType={}", protocolType);
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                            "unknown protocolType=" + protocolType);
            }
        } catch (Exception ex) {
            LOG.error("parse rpc response error", ex);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "parse rpc response error", ex);
        }
        return response;
    }

    public Object[] parseRequestParam(int protocolType, Object body, RpcMethodInfo rpcMethodInfo) {
        if (body == null) {
            return null;
        }

        Object[] args = new Object[rpcMethodInfo.getMethod().getGenericParameterTypes().length];
        if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE) {
            try {
                if (rpcMethodInfo instanceof ProtobufRpcMethodInfo) {
                    ProtobufRpcMethodInfo protobufRpcMethodInfo = (ProtobufRpcMethodInfo) rpcMethodInfo;
                    Message.Builder argBuilder = protobufRpcMethodInfo.getInputInstance().newBuilderForType();
                    jsonPbConverter.merge((String) body, ExtensionRegistry.getEmptyRegistry(), argBuilder);
                    args[0] = argBuilder.build();
                } else {
                    args[0] = gson.fromJson((String) body, rpcMethodInfo.getInputClasses()[0]);
                }
            } catch (Exception e) {
                LOG.error("decodeBody failed", e);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "decode body failed", e);
            }
        } else if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE) {
            Object requestMessage = null;
            try {
                requestMessage = rpcMethodInfo.inputDecode((byte[]) body);
            } catch (Exception ex) {
                LOG.error("invoke protobuf method error, ex : ", ex);
                return null;
            }
            args[0] = requestMessage;
        } else {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "unknown protocol");
        }

        return args;

    }

}