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
package com.baidu.brpc.naming.zookeeper;

import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.protocol.NamingOptions;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.protocol.stargate.StargateConstants;
import com.baidu.brpc.protocol.stargate.StargateURI;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stargate注册时，serviceName 为全小写
 */
@Slf4j
public class StargateZookeeperNamingService extends ZookeeperNamingService {
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEFAULT_GROUP = "normal";
    private static final Set<String> IGNORED_EXTRA_KEYS = new HashSet<String>();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    static {
        IGNORED_EXTRA_KEYS.add(StargateConstants.GROUP_KEY);
        IGNORED_EXTRA_KEYS.add(StargateConstants.VERSION_KEY);
        IGNORED_EXTRA_KEYS.add(StargateConstants.INTERFACE_KEY);
        IGNORED_EXTRA_KEYS.add(StargateConstants.INTERFACE_SIMPLE_KEY);
    }

    public StargateZookeeperNamingService(BrpcURL url) {
        super(url);
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        String path = buildParentNodePath(resolveGroup(subscribeInfo),
                subscribeInfo.getInterfaceName(), resolveVersion(subscribeInfo));
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        try {
            List<String> childList = client.getChildren().forPath(path);
            for (String child : childList) {
                // 跨过所有客户端节点
                if (StargateConstants.ZK_CONSUMER_DIR.equals(child)) {
                    continue;
                }
                String childPath = path + "/" + child;
                try {
                    String childData = new String(client.getData().forPath(childPath));
                    StargateURI uri = new StargateURI.Builder(childData).build();
                    ServiceInstance instance = new ServiceInstance(uri.getHost(), uri.getPort());
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
            log.warn("lookup service instance list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup end point list failed from zookeeper failed", ex);
            }
        }
        return instances;
    }



    @Override
    public void doSubscribe(SubscribeInfo subscribeInfo, final NotifyListener listener) throws Exception {
        final String path = buildParentNodePath(resolveGroup(subscribeInfo), subscribeInfo.getInterfaceName(), resolveVersion(subscribeInfo));
        PathChildrenCache cache = new PathChildrenCache(client, path, true);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                ChildData data = event.getData();
                // 子节点信息，将监听的父节点信息擦除
                String childNodePath = data.getPath().replace(path + "/", "");
                // 如果是客户端上线，不做处理
                if (StargateConstants.ZK_CONSUMER_DIR.equals(childNodePath)) {
                    return;
                }
                switch (event.getType()) {
                    case CHILD_ADDED: {
                        ServiceInstance endPoint = new ServiceInstance(childNodePath);
                        listener.notify(Collections.singletonList(endPoint),
                                Collections.<ServiceInstance>emptyList());
                        break;
                    }
                    case CHILD_REMOVED: {
                        ServiceInstance endPoint = new ServiceInstance(childNodePath);
                        listener.notify(Collections.<ServiceInstance>emptyList(),
                                Collections.singletonList(endPoint));
                        break;
                    }
                    case CHILD_UPDATED:
                    default:
                        break;
                }
            }
        });
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        subscribeCacheMap.putIfAbsent(subscribeInfo, cache);
        log.info("stargate subscribe success from {}", url);
    }

    @Override
    public void doUnsubscribe(SubscribeInfo subscribeInfo) throws Exception {
        super.doUnsubscribe(subscribeInfo);
    }

    @Override
    public void doRegister(RegisterInfo registerInfo) throws Exception {
        String parentPath = buildParentNodePath(resolveGroup(registerInfo), registerInfo.getInterfaceName(), resolveVersion(registerInfo));
        String path = parentPath + "/" + registerInfo.getHost() + ":" + registerInfo.getPort();
        String pathData = buildStarRegisterPathData(registerInfo);
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
        log.info("stargate register success to {}", url);
    }

    @Override
    public void doUnregister(RegisterInfo registerInfo) throws Exception {
        String parentPath = buildParentNodePath(resolveGroup(registerInfo), registerInfo.getInterfaceName(), resolveVersion(registerInfo));
        String path = "/" + registerInfo.getHost() + ":" + registerInfo.getPort();
        client.delete().guaranteed().forPath(parentPath + path);
        log.info("stargate unregister success to {}", url);
    }

    /**
     * stargate 注册或者订阅节点时，整个 Path 为小写
     */
    private String buildParentNodePath(String group, String serviceName, String version) {
        return ("/" + group + ":" + serviceName + ":" + version).toLowerCase();
    }


    /**
     * Build the path data for registration.
     * <p>
     * Stargate protocol requires the data to be a JSON String containing
     * a URI with the following scheme:
     * "star://127.0.0.1:8002?
     * group=normal
     * &interface=com.baidu.brpc.example.stargate.stargatedemoservice
     * &version=1.0.0"
     */
    private String buildStarRegisterPathData(RegisterInfo registerInfo) {
        Map<String, String> extraOptions = registerInfo.getExtra();
        String group = resolveGroup(registerInfo);
        String version = resolveVersion(registerInfo);
        StargateURI.Builder builder = new StargateURI.Builder("star", registerInfo.getHost(), registerInfo.getPort());
        builder.param(StargateConstants.GROUP_KEY, group);
        builder.param(StargateConstants.VERSION_KEY, version);
        builder.param(StargateConstants.INTERFACE_KEY, registerInfo.getInterfaceName());
        if (extraOptions != null) {
            for (Map.Entry<String, String> entry : extraOptions.entrySet()) {
                if (IGNORED_EXTRA_KEYS.contains(entry.getKey())) {
                    continue;
                }
                builder.param(entry.getKey(), entry.getValue());
            }
        }
        String uriString = builder.build().toString();
        return GSON.toJson(uriString);
    }

    private String resolveGroup(NamingOptions info) {
        if (Strings.isNullOrEmpty(info.getGroup())) {
            return DEFAULT_GROUP;
        }
        return info.getGroup();
    }

    private String resolveVersion(NamingOptions info) {
        if (Strings.isNullOrEmpty(info.getVersion())) {
            return DEFAULT_VERSION;
        }
        return info.getVersion();
    }

}
