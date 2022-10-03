package com.baidu.cloud.starlight.benchmark.model;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import java.io.Serializable;

/**
 * Created by liuruisen on 2019/10/29.
 */
@ProtobufClass
public class Experience implements Serializable {

    private String info;


    public Experience(String info) {
        this.info = info;
    }

    public Experience() {
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Experience{");
        sb.append("info='").append(info).append('\'');
        sb.append('}');
        return sb.toString();
    }
}