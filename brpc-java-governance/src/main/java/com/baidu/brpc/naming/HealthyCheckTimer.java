/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.brpc.naming;

import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.thread.ClientHealthCheckTimerInstance;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
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
                boolean isHealthy = isInstanceHealthy(
                        instance.getServiceInstance().getIp(), instance.getServiceInstance().getPort());
                if (isHealthy) {
                    newHealthyInstances.add(instance);
                }
            }

            List<CommunicationClient> newUnhealthyInstances = new ArrayList<CommunicationClient>();
            iter = newUnhealthyInstances.iterator();
            while (iter.hasNext()) {
                CommunicationClient instance = iter.next();
                boolean isHealthy = isInstanceHealthy(
                        instance.getServiceInstance().getIp(), instance.getServiceInstance().getPort());
                if (!isHealthy) {
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
}
