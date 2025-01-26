package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.springcloud.client.StarlightClientAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerEagerLoadProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
@AutoConfigureBefore(value = {LoadBalancerAutoConfiguration.class})
@AutoConfigureAfter(value = {StarlightClientAutoConfiguration.class})
@LoadBalancerClients(defaultConfiguration = StarlightLoadbalancerConfiguration.class)
public class StarlightLoadBalancerAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties,
              ObjectProvider<List<LoadBalancerClientSpecification>> configurations) {
        LoadBalancerClientFactory clientFactory = new StarlightLoadBalancerClientFactory(properties);
        clientFactory.setConfigurations(configurations.getIfAvailable(Collections::emptyList));
        return clientFactory;
    }


    @Bean
    @ConditionalOnBean(LoadBalancerClientFactory.class)
    @ConditionalOnMissingBean
    public LoadBalancerClient weightedBlockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
        return new WeightedBlockLoadBalanceClient(loadBalancerClientFactory);
    }


    @Bean
    public LoadbalancerNamedContextInitializer allEagerContextInitializer(LoadBalancerClientFactory clientFactory,
                                                                    LoadBalancerEagerLoadProperties properties) {
        return new LoadbalancerNamedContextInitializer(clientFactory, properties);
    }
}
