package com.baidu.cloud.starlight.benchmark.model;

public enum Sex {

    MALE("male"), FEMALE("female"), NONE("none");

    private final String name;

    Sex(String name) {
        this.name = name;
    }
}