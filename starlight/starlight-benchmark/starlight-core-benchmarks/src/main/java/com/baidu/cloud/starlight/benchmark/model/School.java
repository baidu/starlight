package com.baidu.cloud.starlight.benchmark.model;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import java.io.Serializable;

/**
 * Created by liuruisen on 2019/10/29.
 */
@ProtobufClass
public class School implements Serializable {

    private String name;

    private Integer age;

    private String location;

    // private String addField = "addField";

    public School() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /** public String getAddField() {
        return addField;
    } **/

    /** public void setAddField(String addField) {
        this.addField = addField;
    } **/

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("School{");
        sb.append("name='").append(name).append('\'');
        sb.append(", age=").append(age);
        sb.append(", location='").append(location).append('\'');
        // sb.append(", addField='").append(addField).append('\'');
        sb.append('}');
        return sb.toString();
    }
}