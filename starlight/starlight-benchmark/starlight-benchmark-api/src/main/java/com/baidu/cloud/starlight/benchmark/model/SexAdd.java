package com.baidu.cloud.starlight.benchmark.model;

/**
 * Created by liuruisen on 2020/9/3.
 */
public enum SexAdd {

    MALE("male"), OTHRE("other"), FEMALE("female"), NONE("none")/*, OTHRE("other")*/;

    private final String name;

    SexAdd(String name) {
        this.name = name;
    }
}
