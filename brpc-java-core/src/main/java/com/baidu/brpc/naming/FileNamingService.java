/**
 * Copyright (c) 2014 Baidu.com, Inc. All Rights Reserved
 * @date Tue Nov 25 13:19:28 CST 2014
 * @author Zhangyi Chen(chenzhangyi01@baidu.com)
 */
package com.baidu.brpc.naming;

import com.baidu.brpc.client.EndPoint;
import com.baidu.brpc.utils.CustomThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fetch service list from File Naming Service
 */
public class FileNamingService implements NamingService {
    private static final Logger LOG = LoggerFactory.getLogger(FileNamingService.class);
    private BrpcURI namingUrl;
    private String filePath;
    private List<EndPoint> lastEndPoints = new ArrayList<EndPoint>();
    private Timer namingServiceTimer;
    private long lastModified;
    private int updateInterval;
    
    public FileNamingService(BrpcURI namingUrl) {
        Validate.notNull(namingUrl);
        Validate.notNull(namingUrl.getPath());
        this.filePath = namingUrl.getPath();
        this.updateInterval = namingUrl.getIntParameter(
                BrpcURI.INTERVAL, BrpcURI.DEFAULT_INTERVAL);
        namingServiceTimer = new HashedWheelTimer(new CustomThreadFactory("namingService-timer-thread"));
    }

    @Override
    public List<EndPoint> lookup(RegisterInfo registerInfo) {
        List<EndPoint> list = new ArrayList<EndPoint>();
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
                EndPoint endPoint = new EndPoint(ipPort[0].trim(),
                        Integer.valueOf(ipPort[1].trim()));
                list.add(endPoint);
            }
            LOG.debug("Got " + list.size() + " servers (out of " + lineNum + ')'
                    + " from " + filePath);
            lastEndPoints = list;
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
    public void subscribe(RegisterInfo registerInfo, final NotifyListener listener) {
        namingServiceTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        try {
                            File file = new File(filePath);
                            long currentModified = file.lastModified();
                            if (currentModified > lastModified) {
                                List<EndPoint> currentEndPoints = lookup(null);
                                Collection<EndPoint> addList = CollectionUtils.subtract(
                                        currentEndPoints, lastEndPoints);
                                Collection<EndPoint> deleteList = CollectionUtils.subtract(
                                        lastEndPoints, currentEndPoints);
                                listener.notify(addList, deleteList);
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
    public void unsubscribe(RegisterInfo registerInfo) {
        namingServiceTimer.stop();
    }

    @Override
    public void register(RegisterInfo registerInfo) {
    }

    @Override
    public void unregister(RegisterInfo registerInfo) {
    }
}
