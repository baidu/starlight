package com.baidu.cloud.starlight.springcloud.lifecycle;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.springcloud.server.StarlightServerAutoConfiguration;
import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterAutoConfiguration;
import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterListener;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
@AutoConfigureAfter(value = {StarlightServerAutoConfiguration.class, StarlightRegisterAutoConfiguration.class})
@ConditionalOnBean(value = {StarlightServer.class, StarlightRegisterListener.class})
public class StarlightServerLifecycleAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public StarlightServerLifecycle starlightServerLifecycle(
            StarlightServer starlightServer, StarlightRegisterListener starlightRegisterListener,
            ApplicationContext applicationContext
    ) {
        return new StarlightServerLifecycle(starlightServer, starlightRegisterListener, applicationContext);
    }
}
