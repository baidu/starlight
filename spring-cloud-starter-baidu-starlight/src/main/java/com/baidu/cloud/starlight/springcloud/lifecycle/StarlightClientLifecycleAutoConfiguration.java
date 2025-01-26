package com.baidu.cloud.starlight.springcloud.lifecycle;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baidu.cloud.starlight.springcloud.client.StarlightClientAutoConfiguration;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;

@Configuration
@AutoConfigureAfter(value = {StarlightClientAutoConfiguration.class})
@ConditionalOnBean(value = {SingleStarlightClientManager.class})
public class StarlightClientLifecycleAutoConfiguration {
    @Bean
    public StarlightClientLifecycle starlightClientLifecycle(
            SingleStarlightClientManager singleStarlightClientManager,
            ApplicationContext applicationContext
    ) {
        return new StarlightClientLifecycle(singleStarlightClientManager, applicationContext);
    }
}
