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

package com.baidu.brpc.naming.consul.client;

import java.util.List;
import java.util.Map;

import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Check;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.agent.model.Service;

/**
 * created by wangsan on 2021/08/18.
 * <p>
 * 补充consul client未能暴露的基于token的接口。以下接口在brpc consul name service中会用到
 *
 * @author wangsan
 */
public interface ConsulClientTokenExt {
    // agent

    Response<Map<String, Check>> getAgentChecks(String token);

    Response<Map<String, Service>> getAgentServices(String token);

    Response<List<Member>> getAgentMembers(String token);

    Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled, String reason,
                                              String token);

    // status

    Response<String> getStatusLeader(String token);

    Response<List<String>> getStatusPeers(String token);

    // health

    Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksForService(String serviceName,
                                                                                     QueryParams queryParams,
                                                                                     String token);

}
