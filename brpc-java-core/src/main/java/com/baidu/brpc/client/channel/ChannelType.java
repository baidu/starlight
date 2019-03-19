package com.baidu.brpc.client.channel;

public enum ChannelType {

    POOLED_CONNECTION(0, "POOLED_CONNECTION"),
    SINGLE_CONNECTION(1, "SINGLE_CONNECTION"),
    SHORT_CONNECTION(2, "SHORT_CONNECTION");

    private int id;
    private String name;

    ChannelType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
