package com.baidu.cloud.starlight.benchmark.model;

public class Address {
    private String address;

    public Address(String address) {
        this.address = address;
    }

    public Address() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Address{");
        sb.append("address='").append(address).append('\'');
        sb.append('}');
        return sb.toString();
    }
}