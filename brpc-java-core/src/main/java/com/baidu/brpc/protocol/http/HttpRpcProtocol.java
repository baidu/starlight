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

import java.util.HashMap;
import java.util.Map;

import com.baidu.brpc.protocol.BrpcMeta;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.RpcMethodInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 处理http rpc协议，包括四种序列化格式：
 * 1、http + protoc
 * 2、http + json
 */
public class HttpRpcProtocol extends AbstractProtocol<HttpBasePacket> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRpcProtocol.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_PROTOBUF = "application/proto";
    /**
     * 请求的唯一标识id
     */
    private static final String LOG_ID = "log-id";
    private static final Gson gson = (new GsonBuilder())
            .serializeNulls()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .create();
    private static final JsonParser jsonParser = new JsonParser();

    private int protocolType;
    private String encoding;

    public HttpRpcProtocol(int protocolType, String encoding) {
        this.protocolType = protocolType;
        this.encoding = encoding;
    }

    @Override
    public FullHttpRequest encodeHttpRequest(RpcRequest rpcRequest) throws Exception {
        String serviceName = rpcRequest.getTargetMethod().getDeclaringClass().getSimpleName();
        String methodName = rpcRequest.getTargetMethod().getName();
        BrpcMeta rpcMeta = rpcRequest.getTargetMethod().getAnnotation(BrpcMeta.class);
        if (rpcMeta != null) {
            serviceName = rpcMeta.serviceName();
            methodName = rpcMeta.methodName();
        }
        LOG.debug("serviceName={}, methodName={}", serviceName, methodName);

        Object requestBody = makeRequest((int) rpcRequest.getLogId(), methodName, rpcRequest.getArgs());
        byte[] requestBodyBytes = encodeBody(protocolType, encoding, requestBody, rpcRequest.getRpcMethodInfo());

        rpcRequest.setUri(buildHttpUri(serviceName, methodName));
        if (requestBodyBytes != null) {
            rpcRequest.content().writeBytes(requestBodyBytes);
        }
        String contentType = HttpRpcProtocol.getContentType(protocolType);
        rpcRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=" + encoding);
        rpcRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, requestBodyBytes == null
                ? 0 : requestBodyBytes.length);
        rpcRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        rpcRequest.headers().set(LOG_ID, rpcRequest.getLogId());
        return rpcRequest;
    }

    @Override
    public RpcResponse decodeHttpResponse(
            FullHttpResponse httpResponse,
            ChannelHandlerContext ctx) {
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        Long logId = parseLogId(httpResponse.headers().get(LOG_ID), channelInfo.getLogId());
        RpcResponse rpcResponse = new RpcResponse(httpResponse);
        rpcResponse.setLogId(logId);
        RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
        if (future == null) {
            return rpcResponse;
        }
        rpcResponse.setRpcFuture(future);

        if (!httpResponse.status().equals(HttpResponseStatus.OK)) {
            LOG.warn("status={}", httpResponse.status());
            rpcResponse.setException(new RpcException(RpcException.SERVICE_EXCEPTION,
                    "http status=" + httpResponse.status()));
            return rpcResponse;
        }

        int bodyLen = httpResponse.content().readableBytes();
        byte[] bytes = new byte[bodyLen];
        httpResponse.content().readBytes(bytes);

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
                rpcResponse.setException(ex);
                return rpcResponse;
            }
        }
        if (body != null) {
            try {
                rpcResponse.setResult(parseHttpResponse(body, future.getRpcMethodInfo()));
            } catch (Exception ex) {
                LOG.error("failed to parse result from HTTP body");
                rpcResponse.setException(ex);
            }
        } else {
            rpcResponse.setResult(null);
        }

        return rpcResponse;
    }

    @Override
    public void decodeHttpRequest(FullHttpRequest httpRequest, RpcRequest rpcRequest) {
        rpcRequest.setHttpRequest(httpRequest);
        httpRequest.release();
        long logId = parseLogId(rpcRequest.headers().get(LOG_ID), null);
        rpcRequest.setLogId(logId);

        String uri = rpcRequest.uri();
        String contentTypeAndEncoding = rpcRequest.headers().get(HttpHeaderNames.CONTENT_TYPE).toLowerCase();
        String[] splits = StringUtils.split(contentTypeAndEncoding, ";");
        int protocolType = HttpRpcProtocol.parseProtocolType(splits[0]);
        String encoding = this.encoding;
        for (String split : splits) {
            split = split.trim();
            if (split.startsWith("charset=")) {
                encoding = split.substring("charset=".length());
            }
        }
        rpcRequest.headers().set(HttpHeaderNames.CONTENT_ENCODING, encoding);

        ByteBuf byteBuf = rpcRequest.content();
        int bodyLen = byteBuf.readableBytes();
        if (bodyLen == 0) {
            String errMsg = String.format("body should not be null, uri:%s", uri);
            LOG.warn(errMsg);
            rpcRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
            return;
        }
        byte[] requestBytes = new byte[bodyLen];
        byteBuf.readBytes(requestBytes, 0, bodyLen);

        Object body = decodeBody(protocolType, encoding, requestBytes);
        rpcRequest.setLogId(logId);

        String serviceName = null;
        String methodName = null;
        if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE
                || protocolType == Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE) {
            String[] uriSplit = uri.split("/");
            if (uriSplit.length < 3) {
                String errMsg = String.format("url format is error, uri:%s", uri);
                LOG.warn(errMsg);
                rpcRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
                return;
            }
            serviceName = uriSplit[uriSplit.length - 2];
            methodName = uriSplit[uriSplit.length - 1];
        } else {
            JsonObject bodyObject = (JsonObject) body;
            methodName = bodyObject.get("method").getAsString();
            serviceName = uri;

        }
        ServiceManager serviceManager = ServiceManager.getInstance();
        RpcMethodInfo rpcMethodInfo = serviceManager.getService(serviceName, methodName);
        if (rpcMethodInfo == null) {
            String errMsg = String.format("Fail to find path=%s", uri);
            LOG.warn(errMsg);
            rpcRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
            return;
        }
        rpcRequest.setRpcMethodInfo(rpcMethodInfo);
        rpcRequest.setTargetMethod(rpcMethodInfo.getMethod());
        rpcRequest.setTarget(rpcMethodInfo.getTarget());

        rpcRequest.setArgs(parseRequestParam(body, rpcMethodInfo));
        return;
    }

    @Override
    public FullHttpResponse encodeHttpResponse(RpcRequest rpcRequest, RpcResponse rpcResponse) {
        if (rpcResponse == null || rpcResponse.getException() != null) {
            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        addHttpHeaders(httpResponse.headers(), rpcRequest);
        Object body = rpcResponse.getResult();

        byte[] responseBytes = null;
        try {
            responseBytes = encodeBody(protocolType,
                    rpcRequest.headers().get(HttpHeaderNames.CONTENT_ENCODING),
                    body, rpcResponse.getRpcMethodInfo());
        } catch (Exception ex3) {
            LOG.warn("encode response failed");
            return null;
        }
        httpResponse.content().writeBytes(responseBytes);
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseBytes.length);

        return httpResponse;
    }

    @Override
    public boolean returnChannelBeforeResponse() {
        return false;
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

    public static String getContentType(Integer protocolType) {
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
                LOG.warn("unkown protocolType={}", protocolType);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                        "unkown protocolType=" + protocolType);
        }
        return contentType;
    }

    protected byte[] encodeBody(int protocolType, String encoding, Object body, RpcMethodInfo rpcMethodInfo) {
        byte[] bodyBytes;
        try {
            switch (protocolType) {
                case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE: {
                    String bodyJson = gson.toJson(body);
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
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "encode body failed");
        }
        return bodyBytes;
    }

    protected Object decodeBody(int protocolType, String encoding, byte[] bytes) {
        Object body = null;
        try {
            switch (protocolType) {
                case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE: {
                    String json = new String(bytes, encoding);
                    body = jsonParser.parse(json);
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
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "decode body failed");
        }
        return body;
    }

    protected void addHttpHeaders(HttpHeaders headers, FullHttpRequest fullHttpRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        if (keepAlive) {
            headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        headers.set(HttpHeaderNames.CONTENT_TYPE, fullHttpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE));
        if (fullHttpRequest.headers().contains("callId")) {
            headers.set("callId", fullHttpRequest.headers().get("callId"));
        }
        if (fullHttpRequest.headers().contains(LOG_ID)) {
            headers.set(LOG_ID, fullHttpRequest.headers().get(LOG_ID));
        }
    }

    protected Object makeRequest(int id, String methodName, Object[] args) {
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

    protected String buildHttpUri(String serviceName, String methodName) {
        // uri格式为 /serviceName/methodName
        return "/" + serviceName + "/" + methodName;
    }

    // 解析log_id
    protected long parseLogId(String headerLogId, Long channelAttachLogId) {
        // 以headerLogId为准，headerLogId为null则以channelAttachLogId为准
        if (headerLogId != null) {
            return Long.valueOf(headerLogId);
        } else if (channelAttachLogId != null) {
            return channelAttachLogId;
        }
        return -1;
    }

    protected Object parseHttpResponse(Object body, RpcMethodInfo rpcMethodInfo) {
        Object response;
        try {
            switch (protocolType) {
                case Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE:
                    response = gson.fromJson((JsonElement) body, rpcMethodInfo.getOutputClass());
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

    protected Object[] parseRequestParam(Object body, RpcMethodInfo rpcMethodInfo) {
        if (body == null) {
            return null;
        }

        Object[] args = new Object[rpcMethodInfo.getMethod().getGenericParameterTypes().length];
        if (protocolType == Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE) {
            args[0] = gson.fromJson((JsonElement) body, rpcMethodInfo.getInputClasses()[0]);
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
