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
 
package com.baidu.cloud.starlight.springcloud.server.register;

import com.baidu.cloud.starlight.api.heartbeat.HeartbeatService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import com.baidu.cloud.starlight.api.utils.NetUriUtils;
import com.baidu.cloud.starlight.core.rpc.RpcServiceRegistry;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Listen to ApplicationStartedEvent and perform service registration Created by liuruisen on 2020/3/2.
 */
public abstract class StarlightRegisterListener implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightRegisterListener.class);

    protected ServiceRegistry serviceRegistry;

    protected ApplicationContext applicationContext;

    protected Registration registration;

    protected StarlightServerProperties serverProperties;

    protected static final String RPC_TYPE = "rpc";

    /**
     * Register and deregister thread pool, single
     */
    private final ExecutorService registerExecutor =
        Executors.newSingleThreadExecutor(new NamedThreadFactory("StarlightRegisterWorker"));

    /**
     * 进行服务注册
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        this.applicationContext = event.getApplicationContext();
        this.serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
        this.serverProperties = applicationContext.getBean(StarlightServerProperties.class);
        Integer registerDelay = serverProperties.getRegisterDelay();
        if (registerDelay != null && registerDelay > 0) {
            try {
                TimeUnit.SECONDS.sleep(registerDelay);
            } catch (InterruptedException e) {
                LOGGER.warn("Exception occur when delay before register, cause by {}", e.getMessage());
            }
        }
        // create StarlightRegistration
        registration = createStarlightRegistration();
        // register StarlightRegistration
        registerExecutor.execute(() -> {
            try {
                LOGGER.info("Register starlight server instance {}:{} start",
                    registration.getHost() == null ? NetUriUtils.getLocalHost() : registration.getHost(),
                    registration.getPort());
                serviceRegistry.register(registration);
                LOGGER.info("Register starlight server instance {}:{} success",
                    registration.getHost() == null ? NetUriUtils.getLocalHost() : registration.getHost(),
                    registration.getPort());
            } catch (Throwable e) {
                LOGGER.warn("Register server instance {}:{} failed, cause by ", registration.getHost(),
                    registration.getPort(), e);
            }
        });

    }

    protected abstract Registration createStarlightRegistration();

    /**
     * Get starlight app name AppName will serve as the service discovery name for SpringCloud
     *
     * @param env
     * @return
     */
    protected String getAppName(Environment env) {
        return ApplicationContextUtils.getApplicationName();
    }

    /**
     * Get port
     *
     * @param env
     * @return
     */
    protected Integer getPort(Environment env) {
        return ApplicationContextUtils.getServerPort();
    }

    /**
     * Generate Starlight InstanceId: appName-ip-port-rpc
     *
     * @param env
     * @return
     */
    protected String getInstanceId(Environment env) {
        String appName = getAppName(env);
        Integer port = getPort(env);
        String instanceId = "instanceId";
        return String.format("%s-%s-%s-rpc", appName, port, instanceId);
    }

    /**
     * Get service interface name that exported to Starlight Server
     *
     * @return
     */
    protected List<String> getInterfaces() {
        List<String> interfaces = new LinkedList<>();
        Set<ServiceInvoker> serviceInvokers = RpcServiceRegistry.getInstance().rpcServices();
        if (serviceInvokers != null) {
            for (ServiceInvoker invoker : serviceInvokers) {
                if (invoker.getRpcService().getServiceClass() != HeartbeatService.class) {
                    interfaces.add(invoker.getRpcService().getServiceClass().getName());
                }
            }
        }
        return interfaces;
    }

    // @PreDestroy
    // The timing of the de-registration call is changed to Gracefully
    public void deRegister() {
        if (registration != null) {
            registerExecutor.execute(() -> {
                try {
                    LOGGER.info("Deregister server instance {}:{} start", registration.getHost(),
                        registration.getPort());
                    serviceRegistry.deregister(registration);
                    LOGGER.info("Deregister server instance {}:{} success", registration.getHost(),
                        registration.getPort());
                } catch (Exception e) {
                    LOGGER.warn("Deregister server instance failed, cause by: ", e);
                }
            });
        }
    }

    protected Map<String, String> starlightMetas() {
        Map<String, String> starlightMetas = new HashMap<>();
        String protocols = applicationContext.getBean(StarlightServerProperties.class).getProtocols();
        starlightMetas.put(SpringCloudConstants.PROTOCOLS_KEY, protocols); // protocols meta
        try {
            List<String> interfaces = getInterfaces(); // interfaces meta
            starlightMetas.put(SpringCloudConstants.INTERFACES_KEY,
                JsonSerializer.OBJECT_MAPPER.writeValueAsString(interfaces));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Add interfaces to register meta failed.", e);
        }
        // epoch represents the start time of the current server
        starlightMetas.put(SpringCloudConstants.EPOCH_KEY, String.valueOf(System.currentTimeMillis()));
        return starlightMetas;
    }

    public void destroy() {
        LOGGER.info("Shutdown the Executor pool for StarlightRegisterListener");
        registerExecutor.shutdown();
    }
}
