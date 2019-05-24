/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.brpc.client.channel;

import java.util.NoSuchElementException;
import java.util.Queue;

import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.protocol.Protocol;

import io.netty.channel.Channel;

public interface BrpcChannel {
    Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException;

    void returnChannel(Channel channel);

    void removeChannel(Channel channel);

    void updateChannel(Channel channel);

    void close();

    Channel connect(final String ip, final int port);

    ServiceInstance getServiceInstance();

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
