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
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.starlight.core.rpc.callback.InvokeCallBack;
import com.baidu.cloud.starlight.core.rpc.callback.SseServerCallBack;
import com.baidu.cloud.starlight.protocol.http.springrest.sse.SpringRestSseProtocol;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Server side Processor Capable of invoke requests and return responses Created by liuruisen on 2020/2/10.
 */
public class ServerProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerProcessor.class);

    private ServiceRegistry serviceRegistry;

    private ThreadPoolFactory threadPoolFactory;

    public ServerProcessor(ServiceRegistry serviceRegistry, ThreadPoolFactory threadPoolFactory) {
        this.serviceRegistry = serviceRegistry;
        this.threadPoolFactory = threadPoolFactory;
    }

    @Override
    public ServiceRegistry getRegistry() {
        return serviceRegistry;
    }

    @Override
    public void process(MsgBase msgBase, RpcChannel context) {

        if (msgBase instanceof Response) {
            LOGGER.error("Received Response message in server side, but is not supported currently");
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "Received Response message in server side, but is not supported currently");
        }

        Request request = (Request) msgBase;
        // construct callback
        RpcCallback callback = Objects.equals(request.getProtocolName(), SpringRestSseProtocol.PROTOCOL_NAME)
            ? new SseServerCallBack(request, context) : new InvokeCallBack(request, context);

        // service check
        ServiceInvoker serviceInvoker = serviceRegistry.discover(request.getServiceName());
        if (serviceInvoker == null) {
            callback.onError(new StarlightRpcException(StarlightRpcException.SERVICE_NOT_FOUND_EXCEPTION,
                "Service {" + request.getServiceName() + "} not found in provider"));
            return;
        }
        RpcService rpcService = serviceInvoker.getRpcService();
        if (rpcService.getServiceConfig() != null && rpcService.getServiceConfig().getInvokeTimeoutMills() != null) {
            // add Timeout
            Integer invokeTimeout = rpcService.getServiceConfig().getInvokeTimeoutMills();
            if (invokeTimeout != null && invokeTimeout > 0) {
                Timeout timeout = TimerHolder.getTimer().newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        callback.onError(new StarlightRpcException(StarlightRpcException.TIME_OUT_EXCEPTION,
                            "Call service {" + request.getServiceName() + "} " + "method {" + request.getMethodName()
                                + "} time out"));
                    }
                }, invokeTimeout, TimeUnit.MILLISECONDS);

                callback.addTimeout(timeout);
            }
        }
        ServerProcessTask task = new ServerProcessTask(request, callback, serviceInvoker);
        LogUtils.addLogTimeAttachment(msgBase, Constants.BEFORE_THREAD_EXECUTE_TIME_KEY, System.currentTimeMillis());
        threadPoolFactory.getThreadPool(rpcService).execute(task);
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
        RpcService rpcService = serviceRegistry.discover(serviceKey).getRpcService();
        if (rpcService != null) {
            return threadPoolFactory.getThreadPool(rpcService).getQueue().size();
        }
        return null; // null: Data is invalid
    }

    @Override
    public Integer processingCount(String serviceKey) {
        RpcService rpcService = serviceRegistry.discover(serviceKey).getRpcService();
        if (rpcService != null) {
            return threadPoolFactory.getThreadPool(rpcService).getActiveCount();
        }
        return null; // null: Data is invalid
    }

    @Override
    public Long completeCount(String serviceKey) {
        RpcService rpcService = serviceRegistry.discover(serviceKey).getRpcService();
        if (rpcService != null) {
            return threadPoolFactory.getThreadPool(rpcService).getCompletedTaskCount();
        }
        return null; // null: Data is invalid
    }

    @Override
    public Integer allWaitTaskCount() {
        ThreadPoolExecutor defaultThreadPool = threadPoolFactory.getThreadPool();
        Integer allWaitTaskCount = defaultThreadPool.getQueue().size() // wait task
            + defaultThreadPool.getActiveCount(); // not complete task

        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();
        for (ServiceInvoker serviceInvoker : registry.rpcServices()) {
            ThreadPoolExecutor serviceThreadPool = threadPoolFactory.getThreadPool(serviceInvoker.getRpcService());
            if (serviceThreadPool != defaultThreadPool) {
                allWaitTaskCount = allWaitTaskCount + serviceThreadPool.getQueue().size() // wait task
                    + serviceThreadPool.getActiveCount(); // not complete task
            }
        }

        return allWaitTaskCount;
    }

    private class ServerProcessTask implements Runnable {

        private Request request;

        private RpcCallback callback;

        private ServiceInvoker serviceInvoker;

        public ServerProcessTask(Request request, RpcCallback callback, ServiceInvoker serviceInvoker) {
            this.request = request;
            this.callback = callback;
            this.serviceInvoker = serviceInvoker;
        }

        @Override
        public void run() {
            Object beforeThreadExeTime = request.getNoneAdditionKv().get(Constants.BEFORE_THREAD_EXECUTE_TIME_KEY);
            if (beforeThreadExeTime instanceof Long) {
                LogUtils.addLogTimeAttachment(request, Constants.WAIT_FOR_THREAD_COST,
                    System.currentTimeMillis() - ((Long) beforeThreadExeTime));
            }

            // request body decode
            RpcService rpcService = serviceInvoker.getRpcService();

            if (!GenericUtil.isGenericCall(request)) {
                Method method = rpcService.getMethod(request.getMethodName());
                if (method == null) {
                    callback.onError(new StarlightRpcException(StarlightRpcException.METHOD_NOT_FOUND_EXCEPTION,
                        "The called method {" + request.getMethodName() + "} does not exist"));
                    return;
                }
                Class<?>[] paramTypes = method.getParameterTypes();
                request.setParamsTypes(paramTypes); // used to deserialize
                request.setGenericParamsTypes(method.getGenericParameterTypes()); // used in springrest
                request.setReturnType(method.getReturnType());
            } else { // generic request
                GenericUtil.markGeneric(request);
            }

            // body decode in work thread
            try {
                Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(request.getProtocolName());
                if (protocol == null) {
                    throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                        "The request's protocol information is not found");
                }
                long beforeTime = System.currentTimeMillis();
                LogUtils.addLogTimeAttachment(request, Constants.BEFORE_DECODE_BODY_TIME_KEY, beforeTime);
                protocol.getDecoder().decodeBody(request);
                LogUtils.addLogTimeAttachment(request, Constants.DECODE_BODY_COST,
                    System.currentTimeMillis() - beforeTime);
            } catch (Exception e) {
                if (e instanceof CodecException) {
                    CodecException codecException = (CodecException) e;
                    e = new CodecException(codecException.getCode(),
                        codecException.getMessage() + " " + Serializer.DESERIALIZE_ERROR_MSG);
                }
                callback.onError(e);
                return;
            }
            // invoke
            LogUtils.addLogTimeAttachment(request, Constants.BEFORE_SERVER_FILTER_EXEC_TIME_KEY,
                System.currentTimeMillis());
            serviceInvoker.invoke(request, callback);
        }
    }
}
