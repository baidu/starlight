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

import com.baidu.brpc.client.channel.BrpcChannelGroup;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface EndPointProcessor {

    void addEndPoints(Collection<EndPoint> addList);

    void deleteEndPoints(Collection<EndPoint> deleteList);

    CopyOnWriteArrayList<BrpcChannelGroup> getHealthyInstances();

    CopyOnWriteArrayList<BrpcChannelGroup> getUnHealthyInstances();

    CopyOnWriteArrayList<EndPoint> getEndPoints();

    void updateUnHealthyInstances(List<BrpcChannelGroup> channelGroups);

    void stop();

}
