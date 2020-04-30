package com.baidu.brpc.spring.boot.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

public class BrpcApplicationContextUtils implements ApplicationContextAware, PriorityOrdered {

    private static ApplicationContext applicationContext;

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BrpcApplicationContextUtils.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        checkApplicationContext();
        return applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        checkApplicationContext();
        return (T) applicationContext.getBean(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> c) {
        checkApplicationContext();
        return applicationContext.getBean(c);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name, Class<T> c) {
        checkApplicationContext();
        return (T) applicationContext.getBean(name, c);
    }

    private static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("applicationContext is null");
        }
    }
}
