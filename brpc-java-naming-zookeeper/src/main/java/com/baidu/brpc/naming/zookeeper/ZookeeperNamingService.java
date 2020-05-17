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

package com.baidu.brpc.naming.zookeeper;

import com.baidu.brpc.client.channel.Endpoint;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.utils.CustomThreadFactory;
import com.baidu.brpc.utils.GsonUtils;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZookeeperNamingService extends FailbackNamingService implements NamingService {
    protected BrpcURL url;
    protected CuratorFramework client;

    protected ConcurrentMap<SubscribeInfo, PathChildrenCache> subscribeCacheMap =
            new ConcurrentHashMap<SubscribeInfo, PathChildrenCache>();

    public ZookeeperNamingService(BrpcURL url) {
        super(url);
        this.url = url;
        int sleepTimeoutMs = url.getIntParameter(
                Constants.SLEEP_TIME_MS, Constants.DEFAULT_SLEEP_TIME_MS);
        int maxTryTimes = url.getIntParameter(
                Constants.MAX_TRY_TIMES, Constants.DEFAULT_MAX_TRY_TIMES);
        int sessionTimeoutMs = url.getIntParameter(
                Constants.SESSION_TIMEOUT_MS, Constants.DEFAULT_SESSION_TIMEOUT_MS);
        int connectTimeoutMs = url.getIntParameter(
                Constants.CONNECT_TIMEOUT_MS, Constants.DEFAULT_CONNECT_TIMEOUT_MS);
        String namespace = Constants.DEFAULT_PATH;
        if (url.getPath().startsWith("/")) {
            namespace = url.getPath().substring(1);
        }
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(sleepTimeoutMs, maxTryTimes);
        client = CuratorFrameworkFactory.builder()
                .connectString(url.getHostPorts())
                .connectionTimeoutMs(connectTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(retryPolicy)
                .namespace(namespace)
                .build();
        client.start();
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        String path = getSubscribePath(subscribeInfo);
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        try {
            List<String> childList = client.getChildren().forPath(path);
            for (String child : childList) {
                String childPath = path + "/" + child;
                try {
                    String childData = new String(client.getData().forPath(childPath));
                    Endpoint endpoint = GsonUtils.fromJson(childData, Endpoint.class);
                    ServiceInstance instance = new ServiceInstance(endpoint);
                    if (subscribeInfo != null && StringUtils.isNoneBlank(subscribeInfo.getServiceId())) {
                        instance.setServiceName(subscribeInfo.getServiceId());
                    }
                    instances.add(instance);
                } catch (Exception getDataFailedException) {
                    log.warn("get child data failed, path:{}, ex:", childPath, getDataFailedException);
                }
            }
            log.info("lookup {} instances from {}", instances.size(), url);
        } catch (Exception ex) {
            log.warn("lookup end point list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup end point list failed from zookeeper failed", ex);
            }
        }
        return instances;
    }






    @Override
    public void doSubscribe(SubscribeInfo subscribeInfo, final NotifyListener listener) throws Exception {
        String path = getSubscribePath(subscribeInfo);
        PathChildrenCache cache = new PathChildrenCache(client, path, true);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                ChildData data = event.getData();
                switch (event.getType()) {
                    case CHILD_ADDED: {
                        ServiceInstance instance = GsonUtils.fromJson(
                                new String(data.getData()), ServiceInstance.class);
                        listener.notify(Collections.singletonList(instance),
                                Collections.<ServiceInstance>emptyList());
                        break;
                    }
                    case CHILD_REMOVED: {
                        ServiceInstance instance = GsonUtils.fromJson(
                                new String(data.getData()), ServiceInstance.class);
                        listener.notify(Collections.<ServiceInstance>emptyList(),
                                Collections.singletonList(instance));
                        break;
                    }
                    case CHILD_UPDATED:
                        break;
                    default:
                        break;
                }
            }
        });
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        subscribeCacheMap.putIfAbsent(subscribeInfo, cache);
        log.info("subscribe success from {}", url);
    }

    @Override
    public void doUnsubscribe(SubscribeInfo subscribeInfo) throws Exception {
        PathChildrenCache cache = subscribeCacheMap.get(subscribeInfo);
        if (cache != null) {
            cache.close();
        }
        subscribeCacheMap.remove(subscribeInfo);
        log.info("unsubscribe success from {}", url);
    }

    @Override
    public void doRegister(RegisterInfo registerInfo) throws Exception {
        String parentPath = getParentRegisterPath(registerInfo);
        String path = getRegisterPath(registerInfo);
        String pathData = getRegisterPathData(registerInfo);
        if (client.checkExists().forPath(parentPath) == null) {
            client.create().withMode(CreateMode.PERSISTENT).forPath(parentPath);
        }
        if (client.checkExists().forPath(path) != null) {
            try {
                client.delete().forPath(path);
            } catch (Exception deleteException) {
                log.info("zk delete node failed, ignore");
            }
        }
        client.create().withMode(CreateMode.EPHEMERAL).forPath(path, pathData.getBytes());
        log.info("register success to {}", url);
    }

    @Override
    public void doUnregister(RegisterInfo registerInfo) throws Exception {
        String path = getRegisterPath(registerInfo);
        client.delete().guaranteed().forPath(path);
        log.info("unregister success to {}", url);
    }

    @Override
    public void destroy() {
        super.destroy();
        client.close();
    }

    public String getSubscribePath(SubscribeInfo subscribeInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        sb.append(subscribeInfo.getGroup()).append(":");
        sb.append(subscribeInfo.getInterfaceName()).append(":");
        sb.append(subscribeInfo.getVersion());
        String path = sb.toString();
        return path;
    }

    public String getParentRegisterPath(RegisterInfo registerInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        sb.append(registerInfo.getGroup()).append(":");
        sb.append(registerInfo.getInterfaceName()).append(":");
        sb.append(registerInfo.getVersion());
        String path = sb.toString();
        return path;
    }

    public String getRegisterPath(RegisterInfo registerInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(getParentRegisterPath(registerInfo));
        sb.append("/");
        sb.append(registerInfo.getHost()).append(":").append(registerInfo.getPort());
        String path = sb.toString();
        return path;
    }

    public String getRegisterPathData(RegisterInfo registerInfo) {
        Endpoint endPoint = new Endpoint(registerInfo.getHost(), registerInfo.getPort());
        return GsonUtils.toJson(endPoint);
    }
}
