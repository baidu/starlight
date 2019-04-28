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

package com.baidu.brpc.client.channel;

import com.baidu.brpc.protocol.Protocol;
import io.netty.channel.Channel;

import java.util.NoSuchElementException;
import java.util.Queue;

public interface BrpcChannel {
    Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException;

    void returnChannel(Channel channel);

    void removeChannel(Channel channel);

    void updateChannel(Channel channel);

    void close();

    Channel connect(final String ip, final int port);

    String getIp();

    int getPort();

    long getFailedNum();

    void incFailedNum();

    Queue<Integer> getLatencyWindow();

    void updateLatency(int latency);

    void updateLatencyWithReadTimeOut();

    Protocol getProtocol();

    void updateMaxConnection(int num);

    int getCurrentMaxConnection();

    int getActiveConnectionNum();

    int getIdleConnectionNum();

    int hashCode();

    boolean equals(Object object);
}
