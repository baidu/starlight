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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.HttpResponse;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.ServiceManager;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 在paas场景下部署时，bns会绑定到主端口上。
 * 如果以servlet方式提供baidu json rpc协议的服务，
 * client端就可以通过bns访问http服务。
 */
public class HttpRpcServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRpcServlet.class);

    public void registerService(Object service) {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(service, null);
    }

    public void registerService(Class targetClass, Object service) {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(targetClass, service, null);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long startTime = System.nanoTime();
        String requestUri = req.getRequestURI();
        if (requestUri == null) {
            LOG.warn("invalid request");
            resp.setStatus(404);
            return;
        }

        String encoding = req.getCharacterEncoding();
        String contentType = req.getContentType().split(";")[0];
        if (contentType == null) {
            contentType = "application/baidu.json-rpc";
        } else {
            contentType = contentType.toLowerCase();
        }

        byte[] bytes = this.readStream(req.getInputStream(), req.getContentLength());

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, requestUri);
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
        httpRequest.content().writeBytes(bytes);
        int protocolType = HttpRpcProtocol.parseProtocolType(contentType);
        Protocol protocol = ProtocolManager.instance().init(null).getProtocol(protocolType);
        Request request = null;
        Response response = new HttpResponse();
        String errorMsg = null;
        try {
            request = protocol.decodeRequest(httpRequest);
            Object result = request.getTargetMethod().invoke(request.getTarget(), request.getArgs());
            response.setResult(result);
            response.setRpcMethodInfo(request.getRpcMethodInfo());
            response.setLogId(request.getLogId());
            protocol.encodeResponse(request, response);
        } catch (Exception ex) {
            errorMsg = String.format("invoke method failed, msg=%s", ex.getMessage());
            LOG.error(errorMsg);
            response.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
        }

        resp.setContentType(contentType);
        resp.setCharacterEncoding(encoding);
        if (errorMsg == null) {
            byte[] content = ((HttpRpcProtocol) protocol).encodeResponseBody(protocolType, request, response);
            resp.setContentLength(content.length);
            resp.getOutputStream().write(content);
        } else {
            byte[] content = errorMsg.getBytes();
            resp.setContentLength(content.length);
            resp.getOutputStream().write(content);
        }

        if (request != null) {
            long endTime = System.nanoTime();
            LOG.debug("uri={} logId={} service={} method={} elapseNs={}",
                    requestUri,
                    request.getLogId(),
                    request.getTarget().getClass().getSimpleName(),
                    request.getTargetMethod().getName(),
                    endTime - startTime);
        }

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
