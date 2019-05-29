package com.baidu.brpc.spring.cloud;

import com.netflix.appinfo.EurekaInstanceConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.DefaultManagementMetadataProvider;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

@Configuration
@AutoConfigureBefore(EurekaClientAutoConfiguration.class)
public class BrpcServiceRegistrationAutoConfiguration implements PriorityOrdered {

    private ConfigurableEnvironment env;

    public BrpcServiceRegistrationAutoConfiguration(ConfigurableEnvironment env) {
        this.env = env;
    }

    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Bean
    @ConditionalOnMissingBean
    public ManagementMetadataProvider serviceManagementMetadataProvider() {
        return new DefaultManagementMetadataProvider();
    }

    @Bean
    @ConditionalOnMissingBean(value = EurekaInstanceConfig.class, search = SearchStrategy.CURRENT)
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(
            InetUtils inetUtils, ManagementMetadataProvider managementMetadataProvider) {
        EurekaInstanceConfigBean instance = new EurekaClientAutoConfiguration(env)
                .eurekaInstanceConfigBean(inetUtils, managementMetadataProvider);

        String brpcPort = env.getProperty("brpc.global.server.port");
        if (StringUtils.isNoneBlank(brpcPort)) {
            instance.getMetadataMap().put("brpcPort", brpcPort);
        }
        return instance;
    }

    private void setupJmxPort(EurekaInstanceConfigBean instance, Integer jmxPort) {
        Map<String, String> metadataMap = instance.getMetadataMap();
        if (metadataMap.get("jmx.port") == null && jmxPort != null) {
            metadataMap.put("jmx.port", String.valueOf(jmxPort));
        }
    }

    private String getProperty(String property) {
        return this.env.containsProperty(property) ? this.env.getProperty(property) : "";
    }
}
