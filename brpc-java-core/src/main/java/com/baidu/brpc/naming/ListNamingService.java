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
package com.baidu.brpc.naming;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.baidu.brpc.client.instance.ServiceInstance;

/**
 * Fetch service list from List Naming Service
 */
public class ListNamingService implements NamingService {
    private List<ServiceInstance> instances;

    public ListNamingService(BrpcURL namingUrl) {
        Validate.notNull(namingUrl);
        Validate.notEmpty(namingUrl.getHostPorts());

        String hostPorts = namingUrl.getHostPorts();
        String[] hostPortSplits = hostPorts.split(",");
        this.instances = new ArrayList<ServiceInstance>(hostPortSplits.length);
        for (String hostPort : hostPortSplits) {
            String[] hostPortSplit = hostPort.split(":");
            String host = hostPortSplit[0];
            int port;
            if (hostPortSplit.length == 2) {
                port = Integer.valueOf(hostPortSplit[1]);
            } else {
                port = 80;
            }
            instances.add(new ServiceInstance(host, port));
        }
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        return instances;
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo, final NotifyListener listener) {
    }

    @Override
    public void unsubscribe(SubscribeInfo subscribeInfo) {
    }

    @Override
    public void register(RegisterInfo registerInfo) {
    }

    @Override
    public void unregister(RegisterInfo registerInfo) {
    }
}
