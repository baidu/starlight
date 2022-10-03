/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.springcloud.common;

import com.baidu.cloud.starlight.springcloud.server.properties.StarlightServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Created by liuruisen on 2020/3/5.
 */
public class ApplicationContextUtils implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextUtils.class);

    private static final String[] WEB_ENVIRONMENT_CLASSES =
        {"javax.servlet.Servlet", "org.springframework.web.context.ConfigurableWebApplicationContext"};

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Get Bean
     * 
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T getBeanByType(Class<T> tClass) {
        if (tClass == null) {
            return null;
        }
        return applicationContext.getBean(tClass);
    }

    /**
     * Get application context
     * 
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Get Environment
     * 
     * @return
     */
    public static Environment getEnvironment() {
        return applicationContext.getEnvironment();
    }

    public static boolean isJarvisEnv() {
        String emProductLine = System.getenv(SpringCloudConstants.EM_PRODUCT_LINE);
        String emPlatform = System.getenv(SpringCloudConstants.EM_PLATFORM);
        String emApp = System.getenv(SpringCloudConstants.EM_APP);

        return !StringUtils.isEmpty(emProductLine) && !StringUtils.isEmpty(emPlatform) && !StringUtils.isEmpty(emApp);
    }

    public static boolean deduceWebEnvironment() {
        for (String className : WEB_ENVIRONMENT_CLASSES) {
            if (!ClassUtils.isPresent(className, null)) {
                return false;
            }
        }
        if (getEnvironment().getProperty("spring.main.web-application-type") != null
            && getEnvironment().getProperty("spring.main.web-application-type").equalsIgnoreCase("none")) {
            return false;
        }

        if (getApplicationContext() instanceof AnnotationConfigApplicationContext) {
            return false;
        }
        return true;
    }

    /**
     * Get starlight port
     *
     * @return
     */
    public static Integer getServerPort() {
        StarlightServerProperties starlightServerProperties = getBeanByType(StarlightServerProperties.class);

        Integer port = starlightServerProperties.getPort();

        if ((port == null || port <= 0) && !deduceWebEnvironment()) {
            port = Integer.parseInt(getEnvironment().getProperty(SpringCloudConstants.SERVER_PORT_KEY, "8080").trim());
        }

        if (port == null || port <= 0) {
            throw new IllegalArgumentException("The following two properties are all illegal, please check: " + "{"
                + SpringCloudConstants.STARLIGHT_SERVER_PORT_KEY + "} " + "{" + SpringCloudConstants.SERVER_PORT_KEY
                + "}");
        }
        return port;
    }

    /**
     * Get application name from environment or env variable
     * 
     * @return
     */
    public static String getApplicationName() {
        // Priority order: starlight.server.name > EM_APP(Jarvis) > spring.application.name

        // starlight.server.name
        String appName = getEnvironment().getProperty(SpringCloudConstants.STARLIGHT_SERVER_NAME_KEY);

        // EM_APP
        if (StringUtils.isEmpty(appName) && ApplicationContextUtils.isJarvisEnv()) {
            // if is jarvis platform, application name will be EM_APP
            appName = System.getenv(SpringCloudConstants.EM_APP);
        }

        // spring.application.name
        if (StringUtils.isEmpty(appName)) {
            appName = getEnvironment().getProperty(SpringCloudConstants.SPRING_APPLICATION_NAME_KEY);
        }

        // default value "application"
        if (StringUtils.isEmpty(appName)) {
            LOGGER.warn(
                "The following three properties are all empty : " + "{" + SpringCloudConstants.STARLIGHT_SERVER_NAME_KEY
                    + "} " + "{" + SpringCloudConstants.SPRING_APPLICATION_NAME_KEY + "}" + "{"
                    + SpringCloudConstants.EM_APP + "}, will use default value {application}");

            appName = "application";
        }

        return appName;
    }
}
