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

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.client.channel.BrpcChannelFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BasicEndpointProcessor implements EndpointProcessor {
    private CopyOnWriteArrayList<Endpoint> endPoints;
    private CopyOnWriteArrayList<BrpcChannel> instances;
    private ConcurrentMap<Endpoint, BrpcChannel> instanceChannelMap;
    private RpcClient rpcClient;

    public BasicEndpointProcessor(RpcClient rpcClient) {
        this.endPoints = new CopyOnWriteArrayList<Endpoint>();
        this.instances = new CopyOnWriteArrayList<BrpcChannel>();
        this.instanceChannelMap = new ConcurrentHashMap<Endpoint, BrpcChannel>();
        this.rpcClient = rpcClient;
    }

    @Override
    public void addEndPoints(Collection<Endpoint> addList) {
        for (Endpoint endPoint : addList) {
            if (!endPoints.contains(endPoint)) {
                endPoints.add(endPoint);
                BrpcChannel brpcChannel = BrpcChannelFactory.createChannel(
                        endPoint.getIp(), endPoint.getPort(), rpcClient);
                instances.add(brpcChannel);
                instanceChannelMap.putIfAbsent(endPoint, brpcChannel);
            }
        }
    }

    @Override
    public void deleteEndPoints(Collection<Endpoint> deleteList) {
        for (Endpoint endPoint : deleteList) {
            if (endPoints.contains(endPoint)) {
                endPoints.remove(endPoint);
                instanceChannelMap.remove(endPoint);
            }
        }
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannel> getHealthyInstances() {
        return instances;
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannel> getUnHealthyInstances() {
        return null;
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

    }

    @Override
    public void stop() {
        for (BrpcChannel channelGroup : instances) {
            channelGroup.close();
        }
    }
}
