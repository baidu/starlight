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
package com.baidu.brpc.spring.cloud;

import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.Constants;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.naming.SubscribeInfo;
import com.baidu.brpc.spring.boot.autoconfigure.BrpcApplicationContextUtils;
import com.baidu.brpc.utils.CustomThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
public class SpringCloudNamingService implements NamingService {
    private List<ServiceInstance> lastInstances = new ArrayList<ServiceInstance>();
    private Timer namingServiceTimer;
    private int updateInterval;
    private DiscoveryClient discoveryClient;

    public SpringCloudNamingService(BrpcURL namingUrl) {
        this.updateInterval = Constants.DEFAULT_INTERVAL;
        namingServiceTimer = new HashedWheelTimer(new CustomThreadFactory("namingService-timer-thread"));
        discoveryClient = BrpcApplicationContextUtils.getBean("discoveryClient", DiscoveryClient.class);
        if (discoveryClient == null) {
            throw new RuntimeException("discovery client is null");
        }
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        List<org.springframework.cloud.client.ServiceInstance> discoveryInstances
                = discoveryClient.getInstances(subscribeInfo.getServiceId());
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        for (org.springframework.cloud.client.ServiceInstance discoveryInstance : discoveryInstances) {
            String host = discoveryInstance.getHost();
            Integer port = Integer.valueOf(discoveryInstance.getMetadata().get(
                    BrpcServiceRegistrationAutoConfiguration.META_DATA_PORT_KEY));
            ServiceInstance instance = new ServiceInstance(host, port);
            instances.add(instance);
        }
        return instances;
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo, final NotifyListener listener) {
        namingServiceTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        try {
                            List<ServiceInstance> currentInstances = lookup(subscribeInfo);
                            Collection<ServiceInstance> addList = CollectionUtils.subtract(
                                    currentInstances, lastInstances);
                            Collection<ServiceInstance> deleteList = CollectionUtils.subtract(
                                    lastInstances, currentInstances);
                            listener.notify(addList, deleteList);
                            lastInstances = currentInstances;
                        } catch (Exception ex) {
                            // ignore exception
                        }
                        namingServiceTimer.newTimeout(this, updateInterval, TimeUnit.MILLISECONDS);

                    }
                },
                updateInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unsubscribe(SubscribeInfo subscribeInfo) {
        namingServiceTimer.stop();
    }

    @Override
    public void register(RegisterInfo registerInfo) {
    }

    @Override
    public void unregister(RegisterInfo registerInfo) {
    }
}
