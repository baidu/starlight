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

package com.baidu.brpc.client.endpoint;

import com.baidu.brpc.client.channel.BrpcChannelFactory;
import com.baidu.brpc.client.channel.BrpcChannelGroup;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcPooledChannel;
import com.baidu.brpc.client.loadbalance.FairStrategy;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EnhancedEndPointProcessor implements EndPointProcessor {

    private RpcClient rpcClient;
    private CopyOnWriteArrayList<BrpcChannelGroup> healthyInstances;
    private CopyOnWriteArrayList<BrpcChannelGroup> unhealthyInstances;
    private CopyOnWriteArrayList<EndPoint> endPoints;
    private Timer healthCheckTimer;
    private volatile boolean isStop = false;

    public EnhancedEndPointProcessor(RpcClient rpcClient) {

        this.rpcClient = rpcClient;
        this.endPoints = new CopyOnWriteArrayList<EndPoint>();
        this.healthyInstances = new CopyOnWriteArrayList<BrpcChannelGroup>();
        this.unhealthyInstances = new CopyOnWriteArrayList<BrpcChannelGroup>();
        healthCheckTimer = ClientHealthCheckTimerInstance.getOrCreateInstance();

        init();
    }

    private void init() {
        // 开启健康检查线程
        healthCheckTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {

                        if (!isStop) {
                            List<BrpcChannelGroup> newHealthyInstances = new ArrayList<BrpcChannelGroup>();
                            Iterator<BrpcChannelGroup> iter = unhealthyInstances.iterator();
                            while (iter.hasNext()) {
                                BrpcChannelGroup instance = iter.next();
                                boolean isHealthy = isInstanceHealthy(instance.getIp(), instance.getPort());
                                if (isHealthy) {
                                    newHealthyInstances.add(instance);
                                }
                            }

                            List<BrpcChannelGroup> newUnhealthyInstances = new ArrayList<BrpcChannelGroup>();
                            iter = healthyInstances.iterator();
                            while (iter.hasNext()) {
                                BrpcChannelGroup instance = iter.next();
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
    public void addEndPoints(Collection<EndPoint> addList) {

        for (EndPoint endPoint : addList) {
            addEndPoint(endPoint);
        }
    }

    @Override
    public void deleteEndPoints(Collection<EndPoint> deleteList) {
        for (EndPoint endPoint : deleteList) {
            deleteEndPoint(endPoint);
        }
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannelGroup> getHealthyInstances() {
        return healthyInstances;
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannelGroup> getUnHealthyInstances() {
        return unhealthyInstances;
    }

    @Override
    public CopyOnWriteArrayList<EndPoint> getEndPoints() {
        return endPoints;
    }

    @Override
    public void updateUnHealthyInstances(List<BrpcChannelGroup> channelGroups) {
        for (BrpcChannelGroup channelGroup : channelGroups) {
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
        for (BrpcChannelGroup channelGroup : healthyInstances) {
            channelGroup.close();
        }
        for (BrpcChannelGroup channelGroup : unhealthyInstances) {
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

    private void addEndPoint(EndPoint endPoint) {
        if (endPoints.contains(endPoint)) {
            log.warn("endpoint already exist, {}:{}", endPoint.getIp(), endPoint.getPort());
            return;
        }
        healthyInstances.add(BrpcChannelFactory.createChannelGroup(endPoint.getIp(), endPoint.getPort(), rpcClient));
        endPoints.add(endPoint);
    }

    private void deleteEndPoint(EndPoint endPoint) {
        List<BrpcChannelGroup> removedInstances = new LinkedList<BrpcChannelGroup>();

        Iterator<BrpcChannelGroup> iterator = healthyInstances.iterator();
        while (iterator.hasNext()) {
            BrpcChannelGroup channelGroup = iterator.next();
            if (channelGroup.getIp().equals(endPoint.getIp())
                    && channelGroup.getPort() == endPoint.getPort()) {
                channelGroup.close();
                healthyInstances.remove(channelGroup);
                removedInstances.add(channelGroup);
                break;
            }
        }

        iterator = unhealthyInstances.iterator();
        while (iterator.hasNext()) {
            BrpcChannelGroup channelGroup = iterator.next();
            if (channelGroup.getIp().equals(endPoint.getIp())
                    && channelGroup.getPort() == endPoint.getPort()) {
                channelGroup.close();
                unhealthyInstances.remove(channelGroup);
                break;
            }
        }
        endPoints.remove(endPoint);

        // notify the fair load balance strategy
        notifyInvalidInstance(removedInstances);
    }

    private void notifyInvalidInstance(List<BrpcChannelGroup> invalidInstances) {
        if (rpcClient.getRpcClientOptions().getLoadBalanceType() == LoadBalanceType.FAIR.getId()) {
            ((FairStrategy) rpcClient.getLoadBalanceStrategy()).markInvalidInstance(invalidInstances);
        }
    }

}
