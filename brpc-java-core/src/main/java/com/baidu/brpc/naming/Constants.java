package com.baidu.brpc.naming;

public class Constants {
    public static final String GROUP = "group";
    public static final String VERSION = "version";

    // update timer interval for pull mode
    public static final String INTERVAL = "interval";
    public static final int DEFAULT_INTERVAL = 1000;

    public static final String SLEEP_TIME_MS = "sleepTimeMs";
    public static final int DEFAULT_SLEEP_TIME_MS = 1000;

    public static final String MAX_TRY_TIMES = "maxTryTimes";
    public static final int DEFAULT_MAX_TRY_TIMES = 3;

    public static final String CONNECT_TIMEOUT_MS = "connectTimeoutMs";
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 1000;

    public static final String SESSION_TIMEOUT_MS = "sessionTimeoutMs";
    public static final int DEFAULT_SESSION_TIMEOUT_MS = 60000;

    public static final String PATH_PREFIX = "prefix";
    public static final String DEFAULT_PATH_PREFIX = "";
}
