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

import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.naming.SubscribeInfo;
import com.baidu.brpc.protocol.stargate.StargateConstants;
import com.baidu.brpc.protocol.stargate.StargateURI;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stargate注册时，serviceName 为全小写
 */
@Slf4j
public class StargateZookeeperNamingService extends ZookeeperNamingService {

    public StargateZookeeperNamingService(BrpcURL url) {
        super(url);
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo info) {
        String path = buildParentNodePath(info.getGroup(), info.getService(), info.getVersion());
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
                    instances.add(new ServiceInstance(uri.getHost(), uri.getPort()));
                } catch (Exception getDataFailedException) {
                    log.warn("get child data failed, path:{}, ex:", childPath, getDataFailedException);
                }
            }
            log.info("lookup {} instances from {}", instances.size(), url);
        } catch (Exception ex) {
            log.warn("lookup service instance list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup end point list failed from zookeeper failed", ex);
            }
        }
        return instances;
    }

    @Override
    public void subscribe(SubscribeInfo info, final NotifyListener listener) {
        try {
            final String path = buildParentNodePath(info.getGroup(), info.getService(), info.getVersion());
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
            failedSubscribes.remove(info);
            subscribeCacheMap.putIfAbsent(info, cache);
            log.info("stargate subscribe success from {}", url);
        } catch (Exception ex) {
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("stargate subscribe failed from " + url, ex);
            } else {
                failedSubscribes.putIfAbsent(info, listener);
            }
        }
    }

    @Override
    public void unsubscribe(SubscribeInfo subscribeInfo) {
        super.unsubscribe(subscribeInfo);
    }

    @Override
    public void register(RegisterInfo info) {
        String parentPath = buildParentNodePath(info.getGroup(), info.getService(), info.getVersion());
        String path = parentPath + "/" + info.getHost() + ":" + info.getPort();
        String pathData = buildStarRegisterPathData(info);
        try {
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
        } catch (Exception ex) {
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("stargate Failed to register to " + url, ex);
            } else {
                failedRegisters.add(info);
                return;
            }
        }
        failedRegisters.remove(info);
    }

    @Override
    public void unregister(RegisterInfo info) {
        String parentPath = buildParentNodePath(info.getGroup(), info.getService(), info.getVersion());
        String path = "/" + info.getHost() + ":" + info.getPort();
        try {
            client.delete().guaranteed().forPath(parentPath + path);
            log.info("stargate unregister success to {}", url);
        } catch (Exception ex) {
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("stargate Failed to unregister from " + url, ex);
            } else {
                failedUnregisters.add(info);
            }
        }
    }

    /**
     * stargate 注册或者订阅节点时，serviceName 为全小写
     */
    private String buildParentNodePath(String group, String serviceName, String version) {
        return "/" + group + ":" + serviceName.toLowerCase() + ":" + version;
    }


    /**
     * "star://127.0.0.1:8002?
     * group=normal
     * &interface=com.baidu.brpc.example.stargate.stargatedemoservice
     * &version=1.0.0"
     */
    private String buildStarRegisterPathData(RegisterInfo registerInfo) {
        return "\"star://" + registerInfo.getHost() + ":" + registerInfo.getPort() + "?" +
                "group=" + registerInfo.getGroup() + "&" +
                "interface=" + registerInfo.getService() + "&" +
                "version=" + registerInfo.getVersion() + "\"";
    }

}
