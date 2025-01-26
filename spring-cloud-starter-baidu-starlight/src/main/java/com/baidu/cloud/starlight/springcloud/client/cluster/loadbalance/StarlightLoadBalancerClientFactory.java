package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

public class StarlightLoadBalancerClientFactory extends LoadBalancerClientFactory {


    private static final String GRAVITY_DISCOVERY_CLIENT =
            "com.baidu.cloud.gravity.discovery.discovery.GravityDiscoveryClient";

    private static final String BNS_DISCOVERY_CLIENT =
            "com.baidu.cloud.bns.discovery.BnsDiscoveryClient";

    @Autowired
    private LoadBalancerClientsProperties properties;

    public StarlightLoadBalancerClientFactory(LoadBalancerClientsProperties properties) {
        super(properties);
    }

    @Override
    public void registerBeans(String name, GenericApplicationContext context) {

        Assert.isInstanceOf(AnnotationConfigRegistry.class, context);
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context;


        if (InstanceUtils.isGravityServiceId(name)){
            // 注册gravity服务发现

            BeanDefinitionBuilder beanDefinitionBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(GRAVITY_DISCOVERY_CLIENT);
            GenericBeanDefinition beanDefinition =
                    new GenericBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
            beanDefinition.setPrimary(true);

            registry.registerBeanDefinition("delegate", beanDefinition);
        } else if (InstanceUtils.isBnsServiceId(name)){
            // 注册bns服务发现
            BeanDefinitionBuilder beanDefinitionBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(BNS_DISCOVERY_CLIENT);
            GenericBeanDefinition beanDefinition =
                    new GenericBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
            beanDefinition.setPrimary(true);

            registry.registerBeanDefinition("delegate", beanDefinition);

        }
        
        super.registerBeans(name, context);
    }

}
