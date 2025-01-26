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
 
package com.baidu.cloud.starlight.springcloud.client.cluster;

import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata and config of the cluster Can selector instance of this cluster Created by liuruisen on 2021/9/6.
 */
public abstract class ClusterSelector {

    /**
     * serviceId of the cluster(app name of provider)
     */
    private String serviceId;

    /**
     * the cluster name or key
     */
    private String clusterName;

    /**
     * 集群标识key-value对
     */
    private Map<String, String> meta = new HashMap<>();

    /**
     * 筛选本集群服务列表
     * 
     * @param originList
     * @return
     */
    public abstract List<ServiceInstance> selectorClusterInstances(List<ServiceInstance> originList);

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    protected Map<String, String> getServerMeta(ServiceInstance server) {
        Map<String, String> meta = new HashMap<>(server.getMetadata());
        if (!meta.containsKey(SpringCloudConstants.EM_PRODUCT_LINE)) { // 真对BNS的兼容
            meta.put(SpringCloudConstants.EM_PRODUCT_LINE, getMeta().get(SpringCloudConstants.EM_PRODUCT_LINE));
            meta.put(SpringCloudConstants.EM_APP, getMeta().get(SpringCloudConstants.EM_APP));
        }

        return meta;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClusterMeta{");
        sb.append("serviceId='").append(serviceId).append('\'');
        sb.append(", clusterName='").append(clusterName).append('\'');
        sb.append(", meta=").append(meta);
        sb.append('}');
        return sb.toString();
    }
}
