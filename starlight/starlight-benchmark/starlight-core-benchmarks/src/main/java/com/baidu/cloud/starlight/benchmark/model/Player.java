package com.baidu.cloud.starlight.benchmark.model;

public enum Player {
    JAVA, FLASH;

    public static Player find(String str) {
        if ("JAVA".equals(str)) {
            return JAVA;
        }
        if ("FLASH".equals(str)) {
            return FLASH;
        }
        String desc = (str == null) ? "NULL" : String.format("'%s'", str);
        throw new IllegalArgumentException("No Player value of " + desc);
    }
}