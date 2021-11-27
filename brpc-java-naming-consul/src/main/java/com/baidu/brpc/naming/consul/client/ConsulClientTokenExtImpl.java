package com.baidu.brpc.naming.consul.client;

import java.util.List;
import java.util.Map;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.transport.RawResponse;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Check;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.agent.model.Service;
import com.ecwid.consul.v1.health.HealthChecksForServiceRequest;
import com.google.gson.reflect.TypeToken;

/**
 * created by wangsan on 2021/08/18.
 *
 * @author wangsan
 */
public class ConsulClientTokenExtImpl implements ConsulClientTokenExt {

    private ConsulRawClient rawClient;

    public ConsulClientTokenExtImpl(ConsulRawClient rawClient) {
        this.rawClient = rawClient;
    }

    @Override
    public Response<Map<String, Check>> getAgentChecks(String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/checks", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            Map<String, Check> value = GsonFactory
                    .getGson().fromJson(rawResponse.getContent(), new TypeToken<Map<String, Check>>() {
                    }.getType());
            return new Response<Map<String, Check>>(value, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Map<String, Service>> getAgentServices(String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/services", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            Map<String, Service> agentServices = GsonFactory.getGson().fromJson(rawResponse.getContent(),
                    new TypeToken<Map<String, Service>>() {
                    }.getType());
            return new Response<Map<String, Service>>(agentServices, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<List<Member>> getAgentMembers(String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/agent/members", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            List<Member> members =
                    GsonFactory.getGson().fromJson(rawResponse.getContent(), new TypeToken<List<Member>>() {
                    }.getType());
            return new Response<List<Member>>(members, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<Void> agentServiceSetMaintenance(String serviceId, boolean maintenanceEnabled, String reason,
                                                     String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
        UrlParameters maintenanceParameter = new SingleUrlParameters("enable", Boolean.toString(maintenanceEnabled));
        UrlParameters reasonParameter = reason != null ? new SingleUrlParameters("reason", reason) : null;

        RawResponse rawResponse = rawClient.makePutRequest("/v1/agent/service/maintenance/" + serviceId, "",
                maintenanceParameter, reasonParameter, tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            return new Response<Void>(null, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<String> getStatusLeader(String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/status/leader", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            String value = GsonFactory.getGson().fromJson(rawResponse.getContent(), String.class);
            return new Response<String>(value, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<List<String>> getStatusPeers(String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
        RawResponse rawResponse = rawClient.makeGetRequest("/v1/status/peers", tokenParam);

        if (rawResponse.getStatusCode() == 200) {
            List<String> value =
                    GsonFactory.getGson().fromJson(rawResponse.getContent(), new TypeToken<List<String>>() {
                    }.getType());
            return new Response<List<String>>(value, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

    @Override
    public Response<List<com.ecwid.consul.v1.health.model.Check>> getHealthChecksForService(String serviceName,
                                                                                            QueryParams queryParams,
                                                                                            String token) {
        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;

        HealthChecksForServiceRequest request = HealthChecksForServiceRequest.newBuilder()
                .setQueryParams(queryParams)
                .build();
        List<UrlParameters> urlParameters = request.asUrlParameters();
        urlParameters.add(tokenParam);

        RawResponse rawResponse = rawClient.makeGetRequest("/v1/health/checks/" + serviceName, urlParameters);

        if (rawResponse.getStatusCode() == 200) {
            List<com.ecwid.consul.v1.health.model.Check> value = GsonFactory.getGson()
                    .fromJson(rawResponse.getContent(), new TypeToken<List<com.ecwid.consul.v1.health.model.Check>>() {
                    }.getType());
            return new Response<List<com.ecwid.consul.v1.health.model.Check>>(value, rawResponse);
        } else {
            throw new OperationException(rawResponse);
        }
    }

}
