/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * 在paas场景下部署时，bns会绑定到主端口上。
 * 如果以servlet方式提供baidu json rpc协议的服务，
 * client端就可以通过bns访问http服务。
 *
 */
public class HttpRpcServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRpcServlet.class);

    public void registerService(Object service) {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(service);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long startTime = System.nanoTime();
        String requestUri = request.getRequestURI();
        if (requestUri == null) {
            LOG.warn("invalid request");
            response.setStatus(404);
            return;
        }

        String encoding = request.getCharacterEncoding();
        String contentType = request.getContentType().split(";")[0];
        if (contentType == null) {
            contentType = "application/baidu.json-rpc";
        } else {
            contentType = contentType.toLowerCase();
        }

        byte[] bytes = this.readStream(request.getInputStream(), request.getContentLength());

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, requestUri);
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
        httpRequest.content().writeBytes(bytes);
        int protocolType = HttpRpcProtocol.parseProtocolType(contentType);
        Protocol protocol = ProtocolManager.instance().init(null).getProtocol(protocolType);
        RpcRequest rpcRequest = RpcRequest.getRpcRequest();
        rpcRequest.reset();
        protocol.decodeHttpRequest(httpRequest, rpcRequest);
        RpcResponse rpcResponse = new RpcResponse();
        try {
            Object result = rpcRequest.getTargetMethod().invoke(
                    rpcRequest.getTarget(), rpcRequest.getArgs()[0]);
            rpcResponse.setResult(result);
        } catch (Exception ex) {
            String errorMsg = String.format("invoke method failed, msg=%s", ex.getMessage());
            LOG.error(errorMsg);
            rpcResponse.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
        }

        FullHttpResponse httpResponse = protocol.encodeHttpResponse(rpcRequest, rpcResponse);

        response.setContentType(contentType);
        response.setContentLength(httpResponse.content().readableBytes());
        response.setCharacterEncoding(encoding);
        response.getOutputStream().write(httpResponse.content().array());

        long endTime = System.nanoTime();
        LOG.debug("uri={} logId={} service={} method={} elapseNs={}",
                requestUri,
                rpcRequest.getLogId(),
                rpcRequest.getTarget().getClass().getSimpleName(),
                rpcRequest.getTargetMethod().getName(),
                endTime - startTime);
    }

    private byte[] readStream(InputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];

        int bytesRead;
        for (int offset = 0; offset < bytes.length; offset += bytesRead) {
            bytesRead = input.read(bytes, offset, bytes.length - offset);
            if (bytesRead == -1) {
                break;
            }
        }

        return bytes;
    }

}
