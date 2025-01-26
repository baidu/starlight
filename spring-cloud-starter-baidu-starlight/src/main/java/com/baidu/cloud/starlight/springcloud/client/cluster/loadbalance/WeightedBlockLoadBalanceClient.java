package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.REQUEST_ROUTE_KEY;

/**
 * 配合权重路由使用的负载均衡客户端
 * @Date 2022/12/9 18:17
 * @Created by liuruisen
 */
public class WeightedBlockLoadBalanceClient extends BlockingLoadBalancerClient {

    private final Map<String, ReactiveLoadBalancer<ServiceInstance>> clusterRules;

    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory;


    public WeightedBlockLoadBalanceClient(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
        super(loadBalancerClientFactory);
        this.clusterRules = new ConcurrentHashMap<>();
        this.loadBalancerClientFactory = loadBalancerClientFactory;
    }


    @Override
    public <T> ServiceInstance choose(String serviceId, Request<T> request) {
        ClusterSelector clusterSelector = RpcContext.getContext().get(REQUEST_ROUTE_KEY);
        if (clusterSelector == null) {
            return super.choose(serviceId, request);
        }

        String subClusterKey = clusterSelector.getClusterName();
        ReactiveLoadBalancer<ServiceInstance> loadBalancer = clusterRules.get(subClusterKey);
        if (loadBalancer == null) {
            loadBalancer = loadBalancerClientFactory.getInstance(serviceId);
            if (loadBalancer == null) {
                return null;
            }
            clusterRules.put(subClusterKey, loadBalancer);
        }

        Response<ServiceInstance> loadBalancerResponse = Mono.from(loadBalancer.choose(request)).block();
        if (loadBalancerResponse == null) {
            return null;
        }
        return loadBalancerResponse.getServer();
    }
}
