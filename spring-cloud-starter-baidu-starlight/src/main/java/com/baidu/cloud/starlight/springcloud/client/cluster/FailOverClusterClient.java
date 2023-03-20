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
 
package com.baidu.cloud.starlight.springcloud.client.cluster;

import com.baidu.cloud.starlight.api.exception.RpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.configuration.Configuration;
import io.netty.util.Timeout;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FailOverClusterClient: when request failed, will retry another instance Created by liuruisen on 2020/9/21.
 */
public class FailOverClusterClient extends AbstractClusterClient {

    /**
     * Key 为 Request的原因是 调用satrgate时会更改requestId
     */
    private final Map<Request, AtomicInteger> remainedRetries = new ConcurrentHashMap<>();

    private final Map<Request, Integer> retryTimesMap = new ConcurrentHashMap<>();

    public FailOverClusterClient(String name, StarlightClientProperties properties, LoadBalancer loadBalancer,
        DiscoveryClient discoveryClient, SingleStarlightClientManager clientManager, Configuration configuration,
        StarlightRouteProperties routeProperties) {
        super(name, properties, loadBalancer, discoveryClient, clientManager, configuration, routeProperties);
    }

    @Override
    public void request(Request request, RpcCallback callback) {
        Integer retryTimes = retryTimes(request);
        if (retryTimes == null || retryTimes <= 0) {
            retryTimes = 0;
        }
        remainedRetries.putIfAbsent(request, new AtomicInteger(retryTimes)); // putIfAbsent
        retryTimesMap.putIfAbsent(request, retryTimes); // putIfAbsent
        super.request(request, new FailOverClusterCallback(callback));
    }

    /**
     * Get retryTimes from properties
     *
     * @return
     */
    private Integer retryTimes(Request request) {
        return properties.getRetryTimes(getName(), request.getServiceClass().getName());
    }

    /**
     * Determined whether method is retryable
     *
     * @return
     */
    private boolean isRetryable(Request request) {
        String retryableMethods = properties.getRetryMethods(getName(), request.getServiceClass().getName());

        if (StringUtils.isEmpty(retryableMethods)) {
            return false;
        }

        String[] retryableMethodList = retryableMethods.split(SpringCloudConstants.RETRYABLE_SPLIT_KEY);
        for (String method : retryableMethodList) {
            if (request.getMethodName().startsWith(method)) {
                return true;
            }
        }

        return false;
    }

    private boolean isRetryable(RpcException rpcException, Request request) {

        String retryErrorCodes = properties.getRetryErrorCodes(getName(), request.getServiceClass().getName());
        if (StringUtils.isEmpty(retryErrorCodes)) {
            return false;
        }

        Integer code = rpcException.getCode();
        if (code == null) {
            return false;
        }

        String strCode = String.valueOf(code);
        String[] retryableCodeList = retryErrorCodes.split(SpringCloudConstants.RETRYABLE_SPLIT_KEY);
        for (String errCode : retryableCodeList) {
            if (strCode.equals(errCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get retry Delay timeunit mills from properties
     *
     * @param request
     * @return
     */
    private Integer retryDelayTimeUnitMills(Request request) {
        return properties.getRetryDelayTimeUnitMills(getName(), request.getServiceClass().getName());
    }

    private void clearRetryTimesCache(Request request) {
        retryTimesMap.remove(request);
        remainedRetries.remove(request);
    }

    protected class FailOverClusterCallback implements RpcCallback {

        private final RpcCallback chainedCallback;

        public FailOverClusterCallback(RpcCallback callback) {
            chainedCallback = callback;
        }

        @Override
        public void addTimeout(Timeout timeout) {
            chainedCallback.addTimeout(timeout);
        }

        @Override
        public Request getRequest() {
            return chainedCallback.getRequest();
        }

        @Override
        public void onResponse(Response response) {
            Integer canRetryTimes = retryTimesMap.get(getRequest());
            AtomicInteger remindRetryTimes = remainedRetries.get(getRequest());
            if (canRetryTimes != null && remindRetryTimes != null && !canRetryTimes.equals(remindRetryTimes.get())) {
                // 代表经历过retry了，记录重试成功日志
                LOGGER.info(
                    "[FailOver] Request retry success:" + " serviceName {}, methodName {}, traceId {}, retryCount {}",
                    getRequest().getServiceName(), getRequest().getMethodName(),
                    LogUtils.parseTraceIdSpanId(getRequest()).get(LogUtils.TCID),
                    (canRetryTimes - remindRetryTimes.get()));
            }
            clearRetryTimesCache(getRequest());
            chainedCallback.onResponse(response);
        }

        @Override
        public void onError(Throwable e) {
            Request request = getRequest();

            if (!(e instanceof RpcException)) {
                clearRetryTimesCache(request);
                chainedCallback.onError(e);
            }

            RpcException rpcException = (RpcException) e;

            // 1. error code can retry, retry
            if (!isRetryable(rpcException, request)) {
                clearRetryTimesCache(request);
                chainedCallback.onError(e);
                return;
            }

            // 2. Method is configured to be retryable, retry
            if (!isRetryable(request)) {
                clearRetryTimesCache(getRequest());
                chainedCallback.onError(e);
                return;
            }

            // 3. reach max retry times, end the request and return exception
            AtomicInteger remindRetryTimes = remainedRetries.get(request);
            // There are no more retries, end the request
            if (remindRetryTimes == null || remindRetryTimes.get() <= 0) {
                LOGGER.warn(
                    "[FailOver] Request failed will not retry, reach the max retry times: "
                        + "serviceName {}, methodName{}, traceId {}",
                    request.getServiceName(), request.getMethodName(),
                    LogUtils.parseTraceIdSpanId(request).get(LogUtils.TCID));
                clearRetryTimesCache(request);
                chainedCallback.onError(e);
                return;
            }

            LOGGER.warn(
                "[FailOver] Request failed will retry: "
                    + "errorCode {}, serviceName {}, methodName {}, traceId {}, retryNo {}, exception {}. ",
                rpcException.getCode(), request.getServiceName(), request.getMethodName(),
                LogUtils.parseTraceIdSpanId(request).get(LogUtils.TCID), remindRetryTimes.get(), e.getMessage());

            try {
                int retryDelayInterval =
                    retryDelayTimeUnitMills(request) * (retryTimesMap.get(request) - remindRetryTimes.get());
                TimeUnit.MILLISECONDS.sleep(retryDelayInterval);
            } catch (InterruptedException interruptedException) {
                LOGGER.error("The delay between two retries was interrupted, this should not happen",
                    interruptedException);
            }

            remindRetryTimes.decrementAndGet(); // decrement retry times

            request(getRequest(), chainedCallback); // retry
        }
    }

}
