package com.baidu.brpc.naming.consul.client;

import java.util.List;
import java.util.Map;

import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Check;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.Self;
import com.ecwid.consul.v1.agent.model.Service;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;

/**
 * created by wangsan on 2021/08/18.
 * 重写ConsulClient， 增加默认token逻辑<br/>
 * 注意：只是选择性的重写了部分高频用到的consul方法，如果需要增强请继续继承该类来扩展
 *
 * @author wangsan
 */
public class ConsulClientDefaultTokenExt extends ConsulClientTokenExt {
    protected ConsulRawClient rawClient;
    protected String token;

    public ConsulClientDefaultTokenExt(ConsulRawClient rawClient) {
        super(rawClient);
        this.rawClient = rawClient;
    }

    public ConsulClientDefaultTokenExt(ConsulRawClient rawClient, String token) {
        super(rawClient);
        this.rawClient = rawClient;
        this.token = token;
    }

    // -------------------------------------------------------------------------------------------
    // Agent

    @Override
    public Response<Self> getAgentSelf() {
        return super.getAgentSelf(token);
    }

    @Override
    public Response<Map<String, Check>> getAgentChecks() {
        return super.getAgentChecks(token);
    }

    @Override
    public Response<Map<String, Service>> getAgentServices() {
        return super.getAgentServices(token);
    }

    @Override
    public Response<List<Member>> getAgentMembers() {
        return super.getAgentMembers(token);
    }

    @Override
    public Response<Void> agentCheckRegister(NewCheck newCheck) {
        return super.agentCheckRegister(newCheck, token);
    }

    @Override
    public Response<Void> agentCheckDeregister(String checkId) {
        return super.agentCheckDeregister(checkId, token);
    }

    @Override
    public Response<Void> agentServiceRegister(NewService newService) {
        return super.agentServiceRegister(newService, token);
    }

    @Override
    public Response<Void> agentServiceDeregister(String serviceId) {
        return super.agentServiceDeregister(serviceId, token);
    }

    @Override
    public Response<Void> agentCheckPass(String checkId) {
        return super.agentCheckPass(checkId, null, token);
    }

    @Override
    public Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled) {
        return super.agentServiceSetMaintenance(serviceId, maintenanceEnabled, null, token);
    }
    // -------------------------------------------------------------------------------------------
    // KV

    @Override
    public Response<Boolean> setKVValue(String key, String value) {
        return setKVValue(key, value, token, null, QueryParams.DEFAULT);
    }

    @Override
    public Response<Boolean> setKVValue(String key, String value, PutParams putParams) {
        return super.setKVValue(key, value, token, putParams);
    }

    @Override
    public Response<Void> deleteKVValue(String key) {
        return super.deleteKVValue(key, token);
    }

    @Override
    public Response<GetValue> getKVValue(String key) {
        return super.getKVValue(key, token);
    }

    // -------------------------------------------------------------------------------------------
    // Session

    @Override
    public Response<String> sessionCreate(NewSession newSession, QueryParams queryParams) {
        return super.sessionCreate(newSession, queryParams, token);
    }

    @Override
    public Response<Void> sessionDestroy(String session, QueryParams queryParams) {
        return super.sessionDestroy(session, queryParams, token);
    }

    @Override
    public Response<Session> renewSession(String session, QueryParams queryParams) {
        return super.renewSession(session, queryParams, token);
    }

    // -------------------------------------------------------------------------------------------
    // Catalog

    @Override
    public Response<Map<String, List<String>>> getCatalogServices(QueryParams queryParams) {
        return super.getCatalogServices(queryParams, token);
    }

    @Override
    public Response<List<CatalogService>> getCatalogService(String serviceName, QueryParams queryParams) {
        return super.getCatalogService(serviceName, queryParams, token);
    }

    // -------------------------------------------------------------------------------------------
    // Health

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, boolean onlyPassing,
                                                           QueryParams queryParams) {
        return super.getHealthServices(serviceName, onlyPassing, queryParams, token);
    }

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, String tag, boolean onlyPassing,
                                                           QueryParams queryParams) {
        return super.getHealthServices(serviceName, tag, onlyPassing, queryParams, token);
    }

    @Override
    public Response<List<HealthService>> getHealthServices(String serviceName, HealthServicesRequest healthRequest) {
        HealthServicesRequest req = HealthServicesRequest.newBuilder()
                .setDatacenter(healthRequest.getDatacenter())
                .setNear(healthRequest.getNear())
                .setTag(healthRequest.getTag())
                .setNodeMeta(healthRequest.getNodeMeta())
                .setPassing(healthRequest.isPassing())
                .setQueryParams(healthRequest.getQueryParams())
                .setToken(healthRequest.getToken() != null ? healthRequest.getToken() : token)
                .build();
        return super.getHealthServices(serviceName, req);
    }

    @Override
    public Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksForService(String serviceName,
                                                                                            QueryParams queryParams) {
        return super.getHealthChecksForService(serviceName, queryParams, token);
    }

    // -------------------------------------------------------------------------------------------
    // Status

    @Override
    public Response<String> getStatusLeader() {
        return super.getStatusLeader(token);
    }

    @Override
    public Response<List<String>> getStatusPeers() {
        return super.getStatusPeers(token);
    }
}

