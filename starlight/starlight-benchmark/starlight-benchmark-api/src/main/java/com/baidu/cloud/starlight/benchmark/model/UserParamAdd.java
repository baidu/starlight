package com.baidu.cloud.starlight.benchmark.model;

import java.util.List;
import java.util.Map;

/**
 * Created by liuruisen on 2020/9/9.
 */
public class UserParamAdd {

    private long userId;

    private Sex sex;

    private int age;

    private String userName;

    private double balance;

    private float salary;

    private AddressAdd address;

    private List<String> tags;

    private List<ExtInfo> extInfos; // proto2 not support map, please transform to list

    private boolean alive;

    private String info;

    private Map<String, Address> map;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<ExtInfo> getExtInfos() {
        return extInfos;
    }

    public void setExtInfos(List<ExtInfo> extInfos) {
        this.extInfos = extInfos;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getSalary() {
        return salary;
    }

    public void setSalary(float salary) {
        this.salary = salary;
    }

    public AddressAdd getAddress() {
        return address;
    }

    public void setAddress(AddressAdd address) {
        this.address = address;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Map<String, Address> getMap() {
        return map;
    }

    public void setMap(Map<String, Address> map) {
        this.map = map;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("userId=").append(userId);
        sb.append(", sex=").append(sex);
        sb.append(", age=").append(age);
        sb.append(", userName='").append(userName).append('\'');
        sb.append(", balance=").append(balance);
        sb.append(", salary=").append(salary);
        sb.append(", address=").append(address);
        sb.append(", tags=").append(tags);
        sb.append(", extInfos=").append(extInfos);
        sb.append(", alive=").append(alive);
        sb.append(", info='").append(info).append('\'');
        sb.append(", map='").append(map).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
