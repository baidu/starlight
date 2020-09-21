package com.baidu.brpc.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * created by wangsan on 2020/6/16.
 *
 * @author wangsan
 */
public class ContextRefreshBean implements ApplicationContextAware, FactoryBean<EchoRequest> {
    ApplicationContext applicationContext;

    @Override
    public EchoRequest getObject() {
        applicationContext.publishEvent(new ContextRefreshedEvent(applicationContext));
        return new EchoRequest();
    }

    @Override
    public Class<EchoRequest> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
