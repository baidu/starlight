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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * Client side processor Used to callback response Created by liuruisen on 2020/2/20.
 */
public class ClientProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProcessor.class);

    private ThreadPoolFactory threadPoolFactory;

    public ClientProcessor(ThreadPoolFactory threadPoolFactory) {
        this.threadPoolFactory = threadPoolFactory;
    }

    @Override
    public ServiceRegistry getRegistry() {
        throw new StarlightRpcException("Client side is not supported ServiceRegistry currently");
    }

    @Override
    public void process(MsgBase msgBase, RpcChannel context) {

        if (msgBase instanceof Request) {
            // log and discard
            LOGGER.error("Received Request message in server side, but is not supported currently");
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "Received Request message in server side, but is not supported currently");
        }
        Response response = (Response) msgBase;

        ClientProcessTask processTask = new ClientProcessTask(response, context);
        LogUtils.addLogTimeAttachment(msgBase, Constants.BEFORE_THREAD_EXECUTE_TIME_KEY, System.currentTimeMillis());
        threadPoolFactory.getThreadPool().execute(processTask);
    }

    @Override
    public void close() {
        if (threadPoolFactory != null) {
            threadPoolFactory.close();
        }
    }

    @Override
    public void setThreadPoolFactory(ThreadPoolFactory threadPoolFactory) {
        this.threadPoolFactory = threadPoolFactory;
    }

    @Override
    public Integer waitTaskCount(String serviceKey) {
        return threadPoolFactory.getThreadPool().getQueue().size();
    }

    @Override
    public Integer processingCount(String serviceKey) {
        return threadPoolFactory.getThreadPool().getActiveCount();
    }

    @Override
    public Long completeCount(String serviceKey) {
        return threadPoolFactory.getThreadPool().getCompletedTaskCount();
    }

    private class ClientProcessTask implements Runnable {

        private Response response;

        private RpcChannel context;

        public ClientProcessTask(Response response, RpcChannel context) {
            this.response = response;
            this.context = context;
        }

        @Override
        public void run() {
            // set currentThread's contextClassLoader to requestThread's ClassLoader
            // this make Protostuff get the correct Schema bound with ClassLoader
            ClassLoader classLoader = LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY)
                .get(context.channel().id().asLongText());
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            } else {
                LOGGER.error("Class Loader related to channel {} is null, plz check",
                    context.channel().id().asLongText());
            }

            Object beforeThreadExeTime = response.getNoneAdditionKv().get(Constants.BEFORE_THREAD_EXECUTE_TIME_KEY);
            if (beforeThreadExeTime instanceof Long) {
                LogUtils.addLogTimeAttachment(response, Constants.WAIT_FOR_THREAD_COST,
                    System.currentTimeMillis() - ((Long) beforeThreadExeTime));
            }

            RpcCallback rpcCallback = context.removeCallback(response.getId());
            if (rpcCallback == null) {
                LogUtils.timeoutReqAdditionalLog(response);
                // FIXME rpcCallback为null是会不会存在别的需要额外处理的情况
                return;
            }

            response.setRequest(rpcCallback.getRequest());
            // response body decode
            Class returnType = rpcCallback.getRequest().getReturnType();
            if (returnType == null) {
                rpcCallback.onError(new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                    "The returnType in the request message is empty. Cannot deserialize the response data!"));
                return;
            }
            response.setReturnType(returnType);
            Type genericType = rpcCallback.getRequest().getGenericReturnType();
            if (genericType == null) {
                rpcCallback.onError(new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                    "The genericReturnType in the request message is empty. "
                        + "Cannot deserialize the response data!"));
                return;
            }
            response.setGenericReturnType(genericType);
            // generic response
            if (GenericUtil.isGenericCall(rpcCallback.getRequest())) {
                GenericUtil.markGeneric(response);
            }
            // body decode in work thread
            try {
                if (response.getStatus() == Constants.SUCCESS_CODE.intValue() && response.getBodyBytes() != null
                    && response.getBodyBytes().length > 0) {
                    Protocol protocol =
                        ExtensionLoader.getInstance(Protocol.class).getExtension(response.getProtocolName());
                    if (protocol == null) {
                        throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                            "The response's protocol information is not found, protocol {" + response.getProtocolName()
                                + "}");
                    }
                    long beforeDecodeBodyTime = System.currentTimeMillis();
                    LogUtils.addLogTimeAttachment(response, Constants.BEFORE_DECODE_BODY_TIME_KEY,
                        beforeDecodeBodyTime);
                    protocol.getDecoder().decodeBody(response);
                    LogUtils.addLogTimeAttachment(response, Constants.DECODE_BODY_COST,
                        System.currentTimeMillis() - beforeDecodeBodyTime);
                }
            } catch (Exception e) {
                if (e instanceof CodecException) {
                    CodecException codecException = (CodecException) e;
                    e = new CodecException(codecException.getCode(),
                        codecException.getMessage() + " " + Serializer.DESERIALIZE_ERROR_MSG);
                }
                rpcCallback.onError(e);
                return;
            }
            // callback
            rpcCallback.onResponse(response);
        }
    }

    @Override
    public Integer allWaitTaskCount() {
        // wait task and running but not complete task
        return waitTaskCount(null) + threadPoolFactory.getThreadPool().getActiveCount();
    }
}
