package com.baidu.cloud.starlight.benchmark.model;

/**
 * Created by liuruisen on 2020/9/9.
 */
public class AddressAdd {

    private String address;

    private String info;

    public AddressAdd(String address) {
        this.address = address;
    }

    public AddressAdd() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AddressAdd{");
        sb.append("address='").append(address).append('\'');
        sb.append(", info='").append(info).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
