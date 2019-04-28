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

package com.baidu.brpc.client.instance;

import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.client.channel.BrpcChannelFactory;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.loadbalance.FairStrategy;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.thread.ClientHealthCheckTimerInstance;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EnhancedEndpointProcessor implements EndpointProcessor {
    private RpcClient rpcClient;
    private CopyOnWriteArrayList<BrpcChannel> healthyInstances;
    private CopyOnWriteArrayList<BrpcChannel> unhealthyInstances;
    private ConcurrentMap<Endpoint, BrpcChannel> instanceChannelMap;
    private CopyOnWriteArrayList<Endpoint> endPoints;
    private Timer healthCheckTimer;
    private volatile boolean isStop = false;

    public EnhancedEndpointProcessor(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
        this.endPoints = new CopyOnWriteArrayList<Endpoint>();
        this.healthyInstances = new CopyOnWriteArrayList<BrpcChannel>();
        this.unhealthyInstances = new CopyOnWriteArrayList<BrpcChannel>();
        this.instanceChannelMap = new ConcurrentHashMap<Endpoint, BrpcChannel>();
        healthCheckTimer = ClientHealthCheckTimerInstance.getOrCreateInstance();
        init();
    }

    private void init() {
        // start healthy check timer
        healthCheckTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        if (!isStop) {
                            List<BrpcChannel> newHealthyInstances = new ArrayList<BrpcChannel>();
                            Iterator<BrpcChannel> iter = unhealthyInstances.iterator();
                            while (iter.hasNext()) {
                                BrpcChannel instance = iter.next();
                                boolean isHealthy = isInstanceHealthy(instance.getIp(), instance.getPort());
                                if (isHealthy) {
                                    newHealthyInstances.add(instance);
                                }
                            }

                            List<BrpcChannel> newUnhealthyInstances = new ArrayList<BrpcChannel>();
                            iter = healthyInstances.iterator();
                            while (iter.hasNext()) {
                                BrpcChannel instance = iter.next();
                                boolean isHealthy = isInstanceHealthy(instance.getIp(), instance.getPort());
                                if (!isHealthy) {
                                    newUnhealthyInstances.add(instance);
                                }
                            }

                            healthyInstances.addAll(newHealthyInstances);
                            unhealthyInstances.removeAll(newHealthyInstances);

                            healthyInstances.removeAll(newUnhealthyInstances);
                            unhealthyInstances.addAll(newUnhealthyInstances);
                            notifyInvalidInstance(newUnhealthyInstances);

                            healthCheckTimer.newTimeout(this,
                                    rpcClient.getRpcClientOptions().getHealthyCheckIntervalMillis(),
                                    TimeUnit.MILLISECONDS);
                        }

                    }
                },
                rpcClient.getRpcClientOptions().getHealthyCheckIntervalMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void addEndPoints(Collection<Endpoint> addList) {
        for (Endpoint endPoint : addList) {
            addEndPoint(endPoint);
        }
    }

    @Override
    public void deleteEndPoints(Collection<Endpoint> deleteList) {
        for (Endpoint endPoint : deleteList) {
            deleteEndPoint(endPoint);
        }
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannel> getHealthyInstances() {
        return healthyInstances;
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannel> getUnHealthyInstances() {
        return unhealthyInstances;
    }

    @Override
    public ConcurrentMap<Endpoint, BrpcChannel> getInstanceChannelMap() {
        return instanceChannelMap;
    }

    @Override
    public CopyOnWriteArrayList<Endpoint> getEndPoints() {
        return endPoints;
    }

    @Override
    public void updateUnHealthyInstances(List<BrpcChannel> channelGroups) {
        for (BrpcChannel channelGroup : channelGroups) {
            healthyInstances.remove(channelGroup);
            if (!unhealthyInstances.contains(channelGroup)) {
                unhealthyInstances.add(channelGroup);
            }
        }

        notifyInvalidInstance(channelGroups);
    }

    @Override
    public void stop() {
        isStop = true;
        for (BrpcChannel channelGroup : healthyInstances) {
            channelGroup.close();
        }
        for (BrpcChannel channelGroup : unhealthyInstances) {
            channelGroup.close();
        }
    }

    private boolean isInstanceHealthy(String ip, int port) {
        boolean isHealthy = false;
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
            isHealthy = true;
        } catch (Exception e) {
            log.warn("Recover socket test for {}:{} failed. message:{}",
                    ip, port, e.getMessage());
            isHealthy = false;
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
            }
        }
        return isHealthy;
    }

    private void addEndPoint(Endpoint endPoint) {
        if (endPoints.contains(endPoint)) {
            log.warn("endpoint already exist, {}:{}", endPoint.getIp(), endPoint.getPort());
            return;
        }
        BrpcChannel brpcChannel = BrpcChannelFactory.createChannel(
                endPoint.getIp(), endPoint.getPort(), rpcClient);
        healthyInstances.add(brpcChannel);
        instanceChannelMap.putIfAbsent(endPoint, brpcChannel);
        endPoints.add(endPoint);
    }

    private void deleteEndPoint(Endpoint endPoint) {
        List<BrpcChannel> removedInstances = new LinkedList<BrpcChannel>();

        Iterator<BrpcChannel> iterator = healthyInstances.iterator();
        while (iterator.hasNext()) {
            BrpcChannel brpcChannel = iterator.next();
            if (brpcChannel.getIp().equals(endPoint.getIp())
                    && brpcChannel.getPort() == endPoint.getPort()) {
                brpcChannel.close();
                healthyInstances.remove(brpcChannel);
                removedInstances.add(brpcChannel);
                break;
            }
        }

        iterator = unhealthyInstances.iterator();
        while (iterator.hasNext()) {
            BrpcChannel channelGroup = iterator.next();
            if (channelGroup.getIp().equals(endPoint.getIp())
                    && channelGroup.getPort() == endPoint.getPort()) {
                channelGroup.close();
                unhealthyInstances.remove(channelGroup);
                break;
            }
        }
        instanceChannelMap.remove(endPoint);
        endPoints.remove(endPoint);

        // notify the fair load balance strategy
        notifyInvalidInstance(removedInstances);
    }

    private void notifyInvalidInstance(List<BrpcChannel> invalidInstances) {
        if (rpcClient.getRpcClientOptions().getLoadBalanceType() == LoadBalanceStrategy.LOAD_BALANCE_FAIR) {
            ((FairStrategy) rpcClient.getLoadBalanceStrategy()).markInvalidInstance(invalidInstances);
        }
    }

}
