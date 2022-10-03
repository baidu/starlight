package com.baidu.cloud.starlight.benchmark.model;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import java.io.Serializable;
// import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by liuruisen on 2019/10/29.
 */
@ProtobufClass
public class Student implements Serializable {

    private long id;

    private String name;

    private Sex sex;

    private int age;

    private float balance;

    private String description;

    private Map<String, String> labels;

    private List<Experience> experiences;

    private School school;

    private double score;

    private Address address;

    private boolean success;

    // private Date date;

    // inner class
    @ProtobufClass
    public static class Address implements Serializable {
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

    public Student(String name) {
        this.experiences = new LinkedList<>();
        this.experiences.add(new Experience("born"));
        this.labels = new HashMap<>();
        this.labels.put("Hobby", "Study");
        this.id = 1234567899000011110l;
        this.name = name;
        this.sex = Sex.MALE;
        this.school = new School();
        this.balance = 123.45f;
        this.age = 20;
        this.score = 89.77777d;
        this.address = new Address("HaiDian");
        this.success = true;
        // this.date = new Date();
    }

    public Student() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<Experience> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<Experience> experiences) {
        this.experiences = experiences;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /** public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    } **/

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Student{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", sex=").append(sex);
        sb.append(", age=").append(age);
        sb.append(", balance=").append(balance);
        sb.append(", description='").append(description).append('\'');
        sb.append(", labels=").append(labels);
        sb.append(", experiences=").append(experiences);
        sb.append(", school=").append(school);
        sb.append(", score=").append(score);
        sb.append(", address=").append(address);
        sb.append(", success=").append(success);
        // sb.append(", date=").append(date);
        sb.append('}');
        return sb.toString();
    }
}
