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

/**
 * Created by liuruisen on 2020/3/2.
 */
public class SpringCloudConstants {

    /**
     * Local host name, used in server side
     */
    public static final String LOCAL_HOST_NAME = "localhost";

    /**
     * StarlightServer bean name
     */
    public static final String STARLIGHT_SERVER_NAME = "StarlightServer";

    /**
     * Used to assemble service bean name
     */
    public static final String BEAN_NAME_SEPARATOR = ":";

    /**
     * Default Cluster model
     */
    public static final String DEFAULT_CLUSTER_MODEL = "failfast";

    /**
     * Default warm up ration 100 present all instance will be init
     */
    public static final Integer DEFAULT_WARM_UP_RATIO = 100;

    /**
     * Client default filters, outlierdetect - context - monitor
     */
    public static final String DEFAULT_CLIENT_FILTERS = "outlierdetect,clientcontext,clientmonitor";
    // + ",formulacircuitbreaker,formularequestdecorate";

    /**
     * Server default filters, context - generic - monitor
     */
    public static final String DEFAULT_SERVER_FILTERS = "servercontext,generic,servermonitor";
    // + ",formularatelimiter";

    /**
     * Cloud Env
     */
    public static final String PAAS_TYPE = "EM_PAAS_TYPE";

    /**
     * CCE container
     */
    public static final String PAAS_TYPE_CCE = "CCE";

    /**
     * BCC container
     */
    public static final String PAAS_TYPE_BCC = "BCC";

    /**
     * Jarvis platform environment variables: EM_APP
     */
    public static final String EM_APP = "EM_APP";

    /**
     * Jarvis platform environment variables: EM_PRODUCT_LINE
     */
    public static final String EM_PRODUCT_LINE = "EM_PRODUCT_LINE";

    /**
     * Jarvis platform environment variables: EM_PLATFORM
     */
    public static final String EM_PLATFORM = "EM_PLATFORM";

    /**
     * starlight.server.name key name
     */
    public static final String STARLIGHT_SERVER_NAME_KEY = "starlight.server.name";

    /**
     * spring application name key name
     */
    public static final String SPRING_APPLICATION_NAME_KEY = "spring.application.name";

    /**
     * starlight server port key name
     */
    public static final String STARLIGHT_SERVER_PORT_KEY = "starlight.server.port";

    /**
     * Server port key
     */
    public static final String SERVER_PORT_KEY = "server.port";

    /**
     * default retry times, used in failover cluster client
     */
    public static final Integer DEFAULT_RETRY_TIMES = 2;

    /**
     * default retry delay mills, used in failover cluster client
     */
    public static final Integer DEFAULT_RETRY_DELAY_MILLS = 100;

    public static final String RETRYABLE_SPLIT_KEY = ",";

    /**
     * Used to identify the server instance launch and register time
     */
    public static final String EPOCH_KEY = "EPOCH";

    public static final String PROTOCOLS_KEY = "protocols";

    public static final String INTERFACES_KEY = "interfaces";

    public static final String OUTLIER_STATS_KEY = "outlier_stats";

    public static final String OUTLIER_DETECT_ENABLED_KEY = "outlier_detect_enabled";

    public static final Boolean OUTLIER_DETECT_ENABLED = false;

    /**
     * Outlier detect: detect interval key
     */
    public static final String OUTLIER_DETECT_INTERVAL_KEY = "outlier_detect_interval";

    /**
     * Outlier detect: default value of outlier detect interval 30s
     */
    public static final Integer OUTLIER_DETECT_INTERVAL = 30;

    /**
     * Outlier detect: Minimum number of requests key
     */
    public static final String OUTLIER_DETECT_MINI_REQUEST_NUM_KEY = "outlier_detect_mini_request_num";

    /**
     * Outlier detect: Default value of minimum number of requests 5
     */
    public static final Integer OUTLIER_DETECT_MINI_REQUEST_NUM = 5;

    /**
     * Outlier detect: Request fail percent threshold key
     */
    public static final String OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY = "outlier_detect_fail_percent_threshold";

    /**
     * Outlier detect: Default value of request fail percent threshold 20%
     */
    public static final Integer OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD = 20;

    /**
     * Outlier detect: Request fail count threshold key
     */
    public static final String OUTLIER_DETECT_FAIL_COUNT_THRESHOLD_KEY = "outlier_detect_fail_count_threshold";

    /**
     * Outlier detect: Default value of request fail count threshold 5
     */
    public static final Integer OUTLIER_DETECT_FAIL_COUNT_THRESHOLD = 5;

    /**
     * Outlier detect: Base eject time key
     */
    public static final String OUTLIER_DETECT_BASE_EJECT_TIME_KEY = "outlier_detect_base_eject_time";

    /**
     * Outlier detect: Default value of base eject time 30s
     */
    public static final Integer OUTLIER_DETECT_BASE_EJECT_TIME = 30;

    /**
     * Outlier detect: Max eject time key
     */
    public static final String OUTLIER_DETECT_MAX_EJECT_TIME_KEY = "outlier_detect_max_eject_time";

    /**
     * Outlier detect: Default value of max eject time 600s
     */
    public static final Integer OUTLIER_DETECT_MAX_EJECT_TIME = 600;

    /**
     * Outlier detect: Max eject percent key
     */
    public static final String OUTLIER_DETECT_MAX_EJECT_PERCENT_KEY = "outlier_detect_max_eject_percent";

    /**
     * Outlier detect: Default value of max eject percent 20%
     */
    public static final Integer OUTLIER_DETECT_MAX_EJECT_PERCENT = 20;

    /**
     * The execution order of shutting down server list filter, third
     */
    public static final Integer SHUTTING_DOWN_SERVER_LIST_FILTER_ORDER = 3;

    /**
     * The execution order of outlier server list filter, second
     */
    public static final Integer OUTLIER_SEVER_LIST_FILTER_ORDER = 2;

    /**
     * The execution order of route server list filter, first
     */
    public static final Integer ROUTE_SERVER_LIST_FILTER_ORDER = 1;

    /**
     * How many times will retry when network error occur
     */
    public static final Integer NETWORK_ERROR_RETRY_TIMES = 3;

    /**
     * Spring cloud heart beat time out
     */
    public static final Integer HEARTBEAT_REQUEST_TIMEOUT = 1000;

    /**
     * Default value of local cache enabled: true
     */
    public static final Boolean LOCAL_CACHE_ENABLED = true;

    /**
     * Default value of whether to store asynchronously
     */
    public static final Boolean STORE_LOCAL_CACHE_ASYNC = false;

    /*
     * The execution order of outlier server list filter, second
     */
    public static final Boolean OUTLIER_RECOVER_BY_CHECK_ENABLED = true;

    /**
     * 用于执行路由
     */
    public static final String REQUEST_ROUTE_KEY = "req_route_meta";

    /**
     * No instance available error code
     */
    public static final Integer NO_INSTANCE_ERROR_CODE = 1008;

    public static final String RDS_KEY = "RDS";

    public static final String LABEL_SELECTOR_ROUTE_KEY = "label_selector_route";

    /**
     * 请求级别的label selector，当前用于海若场景
     */
    public static final String REQUEST_LABEL_SELECTOR_ROUTE_KEY = "request_label_selector_route";
}
