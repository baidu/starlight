/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.serialization.serializer.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by liuruisen on 2020/5/28.
 */
public class User {

    private long userId;

    private Sex sex;

    private int age;

    private String userName;

    private double balance;

    private float salary;

    private Address address;

    private List<String> tags;

    private LinkedList<ExtInfo> extInfos; // proto2 not support map, please transform to list

    private boolean alive;

    private String info;

    private TreeSet<String> set;

    private Object[] array;

    private Map<String, String> map;

    private LocalDateTime birthDay;

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

    public LinkedList<ExtInfo> getExtInfos() {
        return extInfos;
    }

    public void setExtInfos(LinkedList<ExtInfo> extInfos) {
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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
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

    public TreeSet<String> getSet() {
        return set;
    }

    public void setSet(TreeSet<String> set) {
        this.set = set;
    }

    public Object[] getArray() {
        return array;
    }

    public void setArray(Object[] array) {
        this.array = array;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public LocalDateTime getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(LocalDateTime birthDay) {
        this.birthDay = birthDay;
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
        sb.append(", set=").append(set);
        sb.append(", array=").append(Arrays.toString(array));
        sb.append(", map=").append(map);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return userId == user.userId && age == user.age && Double.compare(user.balance, balance) == 0
            && Float.compare(user.salary, salary) == 0 && alive == user.alive && sex == user.sex
            && Objects.equals(userName, user.userName) && Objects.equals(address, user.address)
            && Objects.equals(tags, user.tags) && Objects.equals(extInfos, user.extInfos)
            && Objects.equals(info, user.info) && Objects.equals(set, user.set) && Arrays.equals(array, user.array)
            && Objects.equals(map, user.map);
    }

    @Override
    public int hashCode() {
        int result =
            Objects.hash(userId, sex, age, userName, balance, salary, address, tags, extInfos, alive, info, set, map);
        result = 31 * result + Arrays.hashCode(array);
        return result;
    }
}
