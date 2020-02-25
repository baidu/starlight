/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.brpc.naming;

import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.thread.ClientHealthCheckTimerInstance;
import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by huwenwei on 2019-12-07.
 */
@Slf4j
public class HealthyCheckTimer implements TimerTask {
    private NamingServiceProcessor instanceProcessor;
    private Timer timer = ClientHealthCheckTimerInstance.getOrCreateInstance();
    private int healthyCheckIntervalMillis;
    private boolean stop = false;

    public HealthyCheckTimer(NamingServiceProcessor instanceProcessor,
                             int healthyCheckIntervalMillis) {
        this.instanceProcessor = instanceProcessor;
        this.healthyCheckIntervalMillis = healthyCheckIntervalMillis;
    }

    public void start() {
        timer.newTimeout(this, healthyCheckIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run(Timeout timeout) {
        if (!stop) {
            List<CommunicationClient> newHealthyInstances = new ArrayList<CommunicationClient>();
            Iterator<CommunicationClient> iter = instanceProcessor.getUnhealthyInstances().iterator();
            while (iter.hasNext()) {
                CommunicationClient instance = iter.next();
                boolean isHealthy = isInstanceHealthy(instance);
                if (isHealthy) {
                    // unhealthy has changed to be healthy
                    newHealthyInstances.add(instance);
                }
            }

            List<CommunicationClient> newUnhealthyInstances = new ArrayList<CommunicationClient>();
            iter = instanceProcessor.getHealthyInstances().iterator();
            while (iter.hasNext()) {
                CommunicationClient instance = iter.next();
                boolean isHealthy = isInstanceHealthy(instance);
                if (isHealthy) {
                    // healthy change to be unhealthy
                    newUnhealthyInstances.add(instance);
                }
            }

            instanceProcessor.getLock().lock();
            try {
                if (newUnhealthyInstances.size() > 0) {
                    instanceProcessor.getHealthyInstances().removeAll(newUnhealthyInstances);
                    instanceProcessor.getUnhealthyInstances().addAll(newUnhealthyInstances);
                }

                if (newHealthyInstances.size() > 0) {
                    instanceProcessor.getHealthyInstances().addAll(newHealthyInstances);
                    instanceProcessor.getUnhealthyInstances().removeAll(newHealthyInstances);
                }
            } finally {
                instanceProcessor.getLock().unlock();
            }

            timer.newTimeout(this, healthyCheckIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isInstanceHealthy(CommunicationClient instance) {
        boolean healthy = false;
        try {
            if (instance.getCommunicationOptions().getProtocol().supportHeartbeat()) {
                Request request = new RpcRequest();
                request.reset();
                request.setHeartbeat(true);
                request.setReadTimeoutMillis(instance.getCommunicationOptions().getReadTimeoutMillis());
                request.setWriteTimeoutMillis(instance.getCommunicationOptions().getWriteTimeoutMillis());
                Response response = new RpcResponse();
                response.reset();
                instance.execute(request, response);
                if (response.getException() != null) {
                    throw response.getException();
                }
            } else {
                Channel channel = instance.selectChannel();
                instance.getBrpcChannel().returnChannel(channel);
            }
            healthy = true;
        } catch (Throwable ex) {
            healthy = false;
        }
        log.debug("instance[{}:{}] healthy is {}",
                instance.getServiceInstance().getIp(),
                instance.getServiceInstance().getPort(),
                healthy);
        return healthy;
    }
}
