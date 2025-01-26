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

import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * Through this class, you can obtain and manage all the StarlightClient during the runtime. Singleton, can be used any
 * time the program is running Created by liuruisen on 2020/12/1.
 */
public class SingleStarlightClientManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleStarlightClientManager.class);

    /**
     * The latest SingleStarlightClient
     */
    private final Map<String /* ip:port */, SingleStarlightClient> starlightClients;
    private static SingleStarlightClientManager clientManager;

    private ThreadFactory ioThreadFactory;

    public SingleStarlightClientManager() {
        this.starlightClients = new ConcurrentHashMap<>();
    }

    /**
     * Singleton instance
     * 
     * @return
     */
    public static SingleStarlightClientManager getInstance() {
        if (clientManager == null) {
            synchronized (SingleStarlightClientManager.class) {
                if (clientManager == null) {
                    clientManager = new SingleStarlightClientManager();
                }
            }
        }
        return clientManager;
    }

    /**
     * Get or create SingleStarlightClient Thread-safe
     * 
     * @param host
     * @param port
     * @param config
     * @return
     */
    public SingleStarlightClient getOrCreateSingleClient(String host, Integer port, TransportConfig config,
        Map<Class<?>, ServiceConfig> serviceConfigs) {
        SingleStarlightClient client = getAliveSingleClient(host, port);
        if (client != null) {
            if (serviceConfigs != null && serviceConfigs.size() > 0) {
                for (Map.Entry<Class<?>, ServiceConfig> entry : serviceConfigs.entrySet()) {
                    client.refer(entry.getKey(), entry.getValue());
                }
            }
            return client;
        }

        synchronized (this) {
            client = getAliveSingleClient(host, port);
            if (client != null) {
                return client;
            }
            client = createSingleClient(host, port, config);
            if (serviceConfigs != null && serviceConfigs.size() > 0) {
                for (Map.Entry<Class<?>, ServiceConfig> entry : serviceConfigs.entrySet()) {
                    client.refer(entry.getKey(), entry.getValue());
                }
            }
            // put into manager, or update
            String clientId = InstanceUtils.ipPortStr(host, port);
            starlightClients.put(clientId, client);
        }

        return client;
    }

    public SingleStarlightClient getOrCreateSingleClient(String host, Integer port, TransportConfig config) {
        return getOrCreateSingleClient(host, port, config, null);
    }

    protected SingleStarlightClient createSingleClient(String host, Integer port, TransportConfig config) {
        // init
        SingleStarlightClient singleClient = new SingleStarlightClient(host, port, config, ioThreadFactory);
        singleClient.init();

        return singleClient;
    }

    public SingleStarlightClient getSingleClient(String host, Integer port) {
        return starlightClients.get(InstanceUtils.ipPortStr(host, port));
    }

    public SingleStarlightClient getAliveSingleClient(String host, Integer port) {
        SingleStarlightClient client = getSingleClient(host, port);

        if (client == null) {
            return null;
        }

        if (client.isActive()) {
            return client;
        }

        // 异常实例摘除场景达到最大摘除阈值后，也可返回OUTLIER的实例
        if (client.getStatus() != null && PeerStatus.Status.OUTLIER.equals(client.getStatus().getStatus())) {
            return client;
        }

        return null;
    }

    /**
     * Remove offline client, used in clean up Task
     * 
     * @param host
     * @param port
     */
    public void removeSingleClient(String host, Integer port) {
        SingleStarlightClient client = starlightClients.remove(InstanceUtils.ipPortStr(host, port));
        if (client != null) {
            LOGGER.info("Remove and destroy inactive SingleStarlightClient from StarlightClientManager, "
                + "host {}, port {}, isActive {}", host, port, client.isActive());
            client.destroy(); // gracefully or not
        }
    }

    public Map<String, SingleStarlightClient> allSingleClients() {
        return this.starlightClients;
    }

    /**
     * Destroy all SingleStarlightClients, gracefully shutdown
     */
    public void destroyAll() {
        for (Map.Entry<String, SingleStarlightClient> entry : starlightClients.entrySet()) {
            entry.getValue().destroy();
        }

        starlightClients.clear();
    }

    public void setIoThreadFactory(ThreadFactory ioThreadFactory) {
        this.ioThreadFactory = ioThreadFactory;
    }
}
