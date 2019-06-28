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

package com.baidu.brpc.server;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.JprotobufRpcMethodInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.utils.ProtobufUtils;
import com.baidu.brpc.utils.ThreadPool;

/**
 * Created by huwenwei on 2017/4/25.
 */
@SuppressWarnings("unchecked")
public class ServiceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);
    private static volatile ServiceManager instance;

    private Map<String, RpcMethodInfo> serviceMap;

    public static ServiceManager getInstance() {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager();
                }
            }
        }
        return instance;
    }

    private ServiceManager() {
        this.serviceMap = new HashMap<String, RpcMethodInfo>();
    }

    public void registerService(Object service, ThreadPool threadPool) {
        Class[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length != 1) {
            LOG.error("service must implement one interface only");
            throw new RuntimeException("service must implement one interface only");
        }
        Class clazz = interfaces[0];
        Method[] methods = clazz.getDeclaredMethods();
        registerService(methods, service, threadPool);
    }

    public void registerService(Class targetClass, Object service, ThreadPool threadPool) {
        Class[] interfaces = targetClass.getInterfaces();
        if (interfaces.length != 1) {
            LOG.error("service must implement one interface only");
            throw new RuntimeException("service must implement one interface only");
        }
        Class clazz = interfaces[0];
        Method[] methods = clazz.getDeclaredMethods();
        registerService(methods, service, threadPool);
    }

    public void registerPushService(Object service) {
        registerService(service);
    }

    public void registerService(Object service) {
        Class[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length != 1) {
            LOG.error("service must implement one interface only");
            throw new RuntimeException("service must implement one interface only");
        }
        Class clazz = interfaces[0];
        Method[] methods = clazz.getDeclaredMethods();
        ServiceManager serviceManager = ServiceManager.getInstance();
        for (Method method : methods) {
            RpcMethodInfo serviceInfo = new RpcMethodInfo(method);
            String serviceName = method.getDeclaringClass().getName();
            String methodName = method.getName();
            serviceInfo.setServiceName(serviceName);
            serviceInfo.setMethodName(methodName);
            serviceInfo.setTarget(service);
            serviceInfo.setMethod(method);
            serviceManager.registerService(serviceInfo);
            LOG.info("register service, serviceName={}, methodName={}",
                    serviceInfo.getServiceName(), serviceInfo.getMethodName());
        }
    }

    protected void registerService(Method[] methods, Object service, ThreadPool threadPool) {
        for (Method method : methods) {
            RpcMethodInfo methodInfo;
            ProtobufUtils.MessageType messageType = ProtobufUtils.getMessageType(method);
            if (messageType == ProtobufUtils.MessageType.PROTOBUF) {
                methodInfo = new ProtobufRpcMethodInfo(method);
            } else if (messageType == ProtobufUtils.MessageType.JPROTOBUF) {
                methodInfo = new JprotobufRpcMethodInfo(method);
            } else {
                methodInfo = new RpcMethodInfo(method);
            }
            methodInfo.setTarget(service);
            methodInfo.setThreadPool(threadPool);
            registerService(methodInfo);
            LOG.info("register service, serviceName={}, methodName={}",
                    methodInfo.getServiceName(), methodInfo.getMethodName());
        }
    }

    protected void registerService(RpcMethodInfo methodInfo) {
        String key = buildServiceKey(methodInfo.getServiceName(), methodInfo.getMethodName());
        serviceMap.put(key, methodInfo);
    }

    public RpcMethodInfo getService(String serviceName, String methodName) {
        String key = buildServiceKey(serviceName, methodName);
        return serviceMap.get(key);
    }

    public RpcMethodInfo getService(String serviceMethodName) {
        return serviceMap.get(serviceMethodName);
    }

    public Map<String, RpcMethodInfo> getServiceMap() {
        return serviceMap;
    }


    private String buildServiceKey(String serviceName, String methodName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(serviceName.toLowerCase()).append(".").append(methodName);
        return stringBuilder.toString();
    }
}
