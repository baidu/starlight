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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.utils.CustomThreadFactory;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

public class DnsNamingService implements NamingService {
    private BrpcURL namingUrl;
    private String host;
    private int port;
    private String hostPort;
    private List<ServiceInstance> lastInstances = new ArrayList<ServiceInstance>();
    private Timer namingServiceTimer;
    private int updateInterval;

    public DnsNamingService(BrpcURL namingUrl) {
        Validate.notNull(namingUrl);
        Validate.notEmpty(namingUrl.getHostPorts());
        this.namingUrl = namingUrl;

        String[] splits = namingUrl.getHostPorts().split(":");
        this.host = splits[0];
        if (splits.length == 2) {
            this.port = Integer.valueOf(splits[1]);
        } else {
            this.port = 80;
        }
        this.hostPort = this.host + ":" + this.port;
        this.updateInterval = namingUrl.getIntParameter(
                Constants.INTERVAL, Constants.DEFAULT_INTERVAL);
        namingServiceTimer = new HashedWheelTimer(new CustomThreadFactory("namingService-timer-thread"));
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("unknown http host");
        }

        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        for (InetAddress address : addresses) {
            ServiceInstance instance = new ServiceInstance(address.getHostAddress(), port);
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
                            List<ServiceInstance> currentInstances = lookup(null);
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

    public String getHostPort() {
        return hostPort;
    }
}
