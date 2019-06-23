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
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class DubboZookeeperNamingService extends ZookeeperNamingService {

    public DubboZookeeperNamingService(BrpcURL url) {
        super(url);
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo info) {
        String path = buildParentNodePath(info.getGroup(), info.getInterfaceName(), info.getVersion());
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        try {
            List<String> childList = client.getChildren().forPath(path);
            for (String child : childList) {
                String providerUrlString = URLDecoder.decode(child, "UTF-8");
                BrpcURL url = new BrpcURL(providerUrlString);
                ServiceInstance instance = new ServiceInstance(url.getHostPorts());
                instances.add(instance);
            }
            log.info("lookup {} instances from {}", instances.size(), url);
        } catch (Exception ex) {
            log.warn("lookup service instance list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup service instance list failed from zookeeper", ex);
            }
        }
        return instances;
    }

    @Override
    public void subscribe(SubscribeInfo info, final NotifyListener listener) {
        try {
            final String path = buildParentNodePath(info.getGroup(), info.getInterfaceName(), info.getVersion());
            PathChildrenCache cache = new PathChildrenCache(client, path, true);
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    ChildData data = event.getData();
                    // 子节点信息，将监听的父节点信息擦除
                    String childNodePath = data.getPath().replace(path + "/", "");
                    String providerUrlString = URLDecoder.decode(childNodePath, "UTF-8");
                    BrpcURL url = new BrpcURL(providerUrlString);
                    ServiceInstance instance = new ServiceInstance(url.getHostPorts());
                    switch (event.getType()) {
                        case CHILD_ADDED: {
                            listener.notify(Collections.singletonList(instance),
                                    Collections.<ServiceInstance>emptyList());
                            break;
                        }
                        case CHILD_REMOVED: {
                            listener.notify(Collections.<ServiceInstance>emptyList(),
                                    Collections.singletonList(instance));
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
            log.info("dubbo subscribe success from {}", url);
        } catch (Exception ex) {
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("dubbo subscribe failed from " + url, ex);
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
        String parentPath = buildParentNodePath(info.getGroup(), info.getInterfaceName(), info.getVersion());
        String path = parentPath + "/" + buildRegisterPath(info);
        String pathData = getRegisterPathData(info);
        try {
            if (client.checkExists().forPath(parentPath) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(parentPath);
            }
            if (client.checkExists().forPath(path) != null) {
                try {
                    client.delete().forPath(path);
                } catch (Exception deleteException) {
                    log.info("zk delete node failed, ignore");
                }
            }
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, pathData.getBytes());
            log.info("dubbo register success to {}", url);
        } catch (Exception ex) {
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("dubbo Failed to register to " + url, ex);
            } else {
                failedRegisters.add(info);
                return;
            }
        }
        failedRegisters.remove(info);
    }

    @Override
    public void unregister(RegisterInfo info) {
        String parentPath = buildParentNodePath(info.getGroup(), info.getInterfaceName(), info.getVersion());
        String path = "/" + buildRegisterPath(info);
        try {
            client.delete().guaranteed().forPath(parentPath + path);
            log.info("dubbo unregister success to {}", url);
        } catch (Exception ex) {
            if (!info.isIgnoreFailOfNamingService()) {
                throw new RpcException("dubbo Failed to unregister from " + url, ex);
            } else {
                failedUnregisters.add(info);
            }
        }
    }

    private String buildParentNodePath(String group, String serviceName, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("/")
                .append(serviceName)
                .append("/providers");
        return sb.toString();
    }


    private String buildRegisterPath(RegisterInfo registerInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("dubbo://")
                .append(registerInfo.getHost())
                .append(":")
                .append(registerInfo.getPort())
                .append("/")
                .append(registerInfo.getInterfaceName())
                .append("?")
                .append("interface=")
                .append(registerInfo.getInterfaceName());
        try {
            String url = URLEncoder.encode(sb.toString(), "UTF-8");
            return url;
        } catch (UnsupportedEncodingException ex) {
            log.warn("encode register path failed:", ex);
            throw new RuntimeException("encode register path failed", ex);
        }
    }

}
