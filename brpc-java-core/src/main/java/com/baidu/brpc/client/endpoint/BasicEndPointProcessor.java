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

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannelFactory;
import com.baidu.brpc.client.channel.BrpcChannelGroup;
import com.baidu.brpc.client.channel.BrpcPooledChannel;
import com.baidu.brpc.client.channel.BrpcShortChannel;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BasicEndPointProcessor implements EndPointProcessor {

    private CopyOnWriteArrayList<EndPoint> endPoints;
    private CopyOnWriteArrayList<BrpcChannelGroup> instances;
    private RpcClient rpcClient;

    public BasicEndPointProcessor(RpcClient rpcClient) {
        this.endPoints = new CopyOnWriteArrayList<EndPoint>();
        this.instances = new CopyOnWriteArrayList<BrpcChannelGroup>();
        this.rpcClient = rpcClient;
    }

    @Override
    public void addEndPoints(Collection<EndPoint> addList) {

        for (EndPoint endPoint : addList) {
            if (!endPoints.contains(endPoint)) {
                endPoints.add(endPoint);
                instances.add(BrpcChannelFactory.createChannelGroup(endPoint.getIp(), endPoint.getPort(), rpcClient));
            }
        }
    }

    @Override
    public void deleteEndPoints(Collection<EndPoint> deleteList) {

        for (EndPoint endPoint : deleteList) {
            if (endPoints.contains(endPoint)) {
                endPoints.remove(endPoint);
            }
        }
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannelGroup> getHealthyInstances() {
        return instances;
    }

    @Override
    public CopyOnWriteArrayList<BrpcChannelGroup> getUnHealthyInstances() {
        return null;
    }

    @Override
    public CopyOnWriteArrayList<EndPoint> getEndPoints() {
        return endPoints;
    }

    @Override
    public void updateUnHealthyInstances(List<BrpcChannelGroup> channelGroups) {

    }

    @Override
    public void stop() {
        for (BrpcChannelGroup channelGroup : instances) {
            channelGroup.close();
        }
    }
}
