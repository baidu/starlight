package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.utils.BenchMarkUtil;

import java.util.Arrays;

/**
 * Created by liuruisen on 2019/10/30.
 */
public abstract class StudentMessage<T> {

    private T student; // jmh state
    private byte[] messageBytes; // jmh state
    private String description; // jmh state

    public T getStudent() {
        return student;
    }

    public void setStudent(T student) {
        this.student = student;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }

    public void setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
    }

    public abstract T createStudent(String description);

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StudentMessage{");
        sb.append("student=").append(student);
        sb.append(", messageBytes=").append(Arrays.toString(messageBytes));
        sb.append('}');
        return sb.toString();
    }

    public StudentMessage() {
    }

    public StudentMessage(String messageTxt) {
        if (messageTxt != null && !messageTxt.isEmpty()) {
            // File read: IO operation
            this.description = new String(BenchMarkUtil.initInputMessageBytes(messageTxt));
            this.student = createStudent(this.description);
        }
    }
}