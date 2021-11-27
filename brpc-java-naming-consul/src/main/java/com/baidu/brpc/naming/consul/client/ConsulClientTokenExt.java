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
 * 补充consul client未能暴露的基于token的接口。一下接口在brpc consul name service会用到
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
