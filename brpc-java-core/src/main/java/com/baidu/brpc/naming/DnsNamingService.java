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

package com.baidu.brpc.naming;

import com.baidu.brpc.client.EndPoint;
import com.baidu.brpc.utils.CustomThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DnsNamingService implements NamingService {
    private BrpcURI namingUrl;
    private String host;
    private int port;
    private String hostPort;
    private List<EndPoint> lastEndPoints = new ArrayList<EndPoint>();
    private Timer namingServiceTimer;
    private int updateInterval;

    public DnsNamingService(BrpcURI namingUrl) {
        Validate.notNull(namingUrl);
        Validate.notEmpty(namingUrl.getHosts());
        this.namingUrl = namingUrl;
        this.host = namingUrl.getHosts().get(0);
        String portString = namingUrl.getPorts().get(0);
        if (StringUtils.isNotBlank(portString)) {
            this.port = Integer.valueOf(portString);
        } else {
            this.port = 80;
        }
        this.hostPort = this.host + ":" + this.port;
        this.updateInterval = namingUrl.getIntParameter(
                BrpcURI.INTERVAL, BrpcURI.DEFAULT_INTERVAL);
        namingServiceTimer = new HashedWheelTimer(new CustomThreadFactory("namingService-timer-thread"));
    }

    @Override
    public List<EndPoint> lookup(RegisterInfo registerInfo) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("unknown http host");
        }

        List<EndPoint> endPoints = new ArrayList<EndPoint>();
        for (InetAddress address : addresses) {
            EndPoint endPoint = new EndPoint(address.getHostAddress(), port);
            endPoints.add(endPoint);
        }
        this.lastEndPoints = endPoints;
        return endPoints;
    }

    @Override
    public void subscribe(RegisterInfo registerInfo, final NotifyListener listener) {
        namingServiceTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        try {
                            List<EndPoint> currentEndPoints = lookup(null);
                            Collection<EndPoint> addList = CollectionUtils.subtract(
                                    currentEndPoints, lastEndPoints);
                            Collection<EndPoint> deleteList = CollectionUtils.subtract(
                                    lastEndPoints, currentEndPoints);
                            listener.notify(addList, deleteList);
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
    public void register(RegisterInfo url) {
    }

    @Override
    public void unregister(RegisterInfo url) {
    }

    public String getHostPort() {
        return hostPort;
    }
}
