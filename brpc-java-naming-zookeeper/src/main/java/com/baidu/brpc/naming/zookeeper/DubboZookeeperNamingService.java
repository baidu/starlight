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
import com.baidu.brpc.protocol.SubscribeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        String path = buildParentNodePath(subscribeInfo.getGroup(),
                subscribeInfo.getInterfaceName(), subscribeInfo.getVersion());
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        try {
            List<String> childList = client.getChildren().forPath(path);
            for (String child : childList) {
                String providerUrlString = URLDecoder.decode(child, "UTF-8");
                BrpcURL url = new BrpcURL(providerUrlString);
                ServiceInstance instance = new ServiceInstance(url.getHostPorts());
                if (subscribeInfo != null && StringUtils.isNoneBlank(subscribeInfo.getServiceId())) {
                    instance.setServiceName(subscribeInfo.getServiceId());
                }
                instances.add(instance);
            }
            log.info("lookup {} instances from {}", instances.size(), url);
        } catch (Exception ex) {
            log.warn("lookup service instance list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup service instance list failed from zookeeper", ex);
            }
        }
        return instances;
    }



    @Override
    public void doSubscribe(SubscribeInfo subscribeInfo, final NotifyListener listener) throws Exception {
        final String path = buildParentNodePath(subscribeInfo.getGroup(), subscribeInfo.getInterfaceName(), subscribeInfo.getVersion());
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
        subscribeCacheMap.putIfAbsent(subscribeInfo, cache);
        log.info("dubbo subscribe success from {}", url);
    }

    @Override
    public void doUnsubscribe(SubscribeInfo subscribeInfo) throws Exception {
        super.doUnsubscribe(subscribeInfo);
    }

    @Override
    public void doRegister(RegisterInfo registerInfo) throws Exception {
        String parentPath = buildParentNodePath(registerInfo.getGroup(), registerInfo.getInterfaceName(), registerInfo.getVersion());
        String path = parentPath + "/" + buildRegisterPath(registerInfo);
        String pathData = getRegisterPathData(registerInfo);
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
    }

    @Override
    public void doUnregister(RegisterInfo registerInfo) throws Exception {
        String parentPath = buildParentNodePath(registerInfo.getGroup(), registerInfo.getInterfaceName(), registerInfo.getVersion());
        String path = "/" + buildRegisterPath(registerInfo);
        client.delete().guaranteed().forPath(parentPath + path);
        log.info("dubbo unregister success to {}", url);
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
                .append("?category=providers")
                .append("&interface=")
                .append(registerInfo.getInterfaceName());
        if (StringUtils.isNotBlank(registerInfo.getGroup())) {
            sb.append("&group=").append(registerInfo.getGroup());
        }
        if (StringUtils.isNotBlank(registerInfo.getVersion())) {
            sb.append("&group=").append(registerInfo.getVersion());
        }
        try {
            String url = URLEncoder.encode(sb.toString(), "UTF-8");
            return url;
        } catch (UnsupportedEncodingException ex) {
            log.warn("encode register path failed:", ex);
            throw new RuntimeException("encode register path failed", ex);
        }
    }

}
