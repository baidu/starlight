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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.utils.CustomThreadFactory;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/**
 * Fetch service list from File Naming Service
 */
public class FileNamingService implements NamingService {
    private static final Logger LOG = LoggerFactory.getLogger(FileNamingService.class);
    private BrpcURL namingUrl;
    private String filePath;
    private List<ServiceInstance> lastInstances = new ArrayList<ServiceInstance>();
    private Timer namingServiceTimer;
    private long lastModified;
    private int updateInterval;
    
    public FileNamingService(BrpcURL namingUrl) {
        Validate.notNull(namingUrl);
        Validate.notNull(namingUrl.getPath());
        this.namingUrl = namingUrl;
        this.filePath = namingUrl.getPath();
        this.updateInterval = namingUrl.getIntParameter(
                Constants.INTERVAL, Constants.DEFAULT_INTERVAL);
        namingServiceTimer = new HashedWheelTimer(new CustomThreadFactory("namingService-timer-thread"));
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        List<ServiceInstance> list = new ArrayList<ServiceInstance>();
        int lineNum = 0;
        BufferedReader reader = null;
        try {
            File file = new File(filePath);
            long lastModified = file.lastModified();
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                String[] ipPort = line.split(":");
                if (ipPort.length != 2) {
                    LOG.warn("Invalid address format: " + line);
                    continue;
                }
                ServiceInstance instance = new ServiceInstance(ipPort[0].trim(),
                        Integer.valueOf(ipPort[1].trim()));
                list.add(instance);
            }
            LOG.debug("Got " + list.size() + " servers (out of " + lineNum + ')'
                    + " from " + filePath);
            this.lastModified = lastModified;
            return list;
        } catch (IOException ex) {
            LOG.warn("read file error, fileName={}", filePath);
            throw new RuntimeException("read naming file error");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex2) {
                    LOG.warn("close failed");
                }
            }
        }
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo, final NotifyListener listener) {
        namingServiceTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        try {
                            File file = new File(filePath);
                            long currentModified = file.lastModified();
                            if (currentModified > lastModified) {
                                List<ServiceInstance> currentInstances = lookup(null);
                                Collection<ServiceInstance> addList = CollectionUtils.subtract(
                                        currentInstances, lastInstances);
                                Collection<ServiceInstance> deleteList = CollectionUtils.subtract(
                                        lastInstances, currentInstances);
                                listener.notify(addList, deleteList);
                                lastInstances = currentInstances;
                            }
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
}
