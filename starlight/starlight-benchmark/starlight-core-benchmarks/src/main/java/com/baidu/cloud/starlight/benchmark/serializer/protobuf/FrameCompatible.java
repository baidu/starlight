package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.Student;
import com.baidu.cloud.starlight.benchmark.model.StudentProto;
import com.baidu.cloud.starlight.benchmark.model.StudentProto3;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;

/**
 * Created by liuruisen on 2019/10/30.
 * 测试兼容性
 * - 三种框架的兼容性
 */
public class FrameCompatible extends BaseCompatible {

    public static void main(String[] args) {
        jdkSerialize();
        System.out.println("-----Jprotobuf To Protobuf2 : " + jprotobufToProtobuf2()); // true
        System.out.println("-----Jprotobuf To Protobuf3 : " + jprotobufToProtobuf3()); // true 丢失 bool

        System.out.println("-----Protostuff To Protobuf2 : " + protostuffToProtobuf2()); // true 丢失 bool
        System.out.println("-----Protostuff To Protobuf3 : " + protostuffToProtobuf3()); // true 丢失 bool

        System.out.println("-----Protostuff To Jprotobuf : " + protostuffToJprotobuf()); // ture  List Map Reference 嵌套
        System.out.println("-----Jprotobuf To Protostuff : " + jprotobufToProtostuff()); // false

        System.out.println("-----Protobuf2 To Jprotobuf : " + protobuf2ToJprotobuf()); // 丢失 bool address
        System.out.println("-----Protobuf3 To Jprotobuf : " + protobuf3ToJprotobuf()); // 丢失 bool address

        System.out.println("-----Protobuf2 To Protostuff : " + protobuf2ToProtostuff()); // false
        System.out.println("-----Protobuf3 To Protostuff : " + protobuf3ToProtostuff()); // false

    }

    /**
     * jprotobuf bytes deserialize by protobuff-java proto2
     */
    public static boolean jprotobufToProtobuf2() {

        // jprotobuf serialize
        byte[] messageBytes = jprotobufSerialize();

        try {
            // protobuf deserialize
            StudentProto.Student student = protobuf2Deserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (InvalidProtocolBufferException e) {
            System.out.println(e);
            return false;
        }
        return false;
    }

    /**
     * jprotobuf bytes deserialize by protobuff-java proto3
     */
    public static boolean jprotobufToProtobuf3() {

        // jprotobuf serialize
        byte[] messageBytes = jprotobufSerialize();

        try {
            // protobuf deserialize
            StudentProto3.Student student = protobuf3Deserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (InvalidProtocolBufferException e) {
            System.out.println(e);
            return false;
        }
        return false;
    }


    /**
     * protostuff bytes deserialize by protobuff-java proto2
     */
    public static boolean protostuffToProtobuf2() {

        // ProtoStuff serialize
        byte[] messageBytes = protostuffSerialize();

        try {
            // Protobuf deserialize
            StudentProto.Student student = protobuf2Deserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (InvalidProtocolBufferException e) {
            System.out.println(e);
            return false;
        }

        return false;
    }


    /**
     * protostuff bytes deserialize by protobuff-java proto3
     */
    public static boolean protostuffToProtobuf3() {

        // ProtoStuff serialize
        byte[] messageBytes = protostuffSerialize();

        try {
            // Protobuf deserialize
            StudentProto3.Student student = protobuf3Deserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (InvalidProtocolBufferException e) {
            System.out.println(e);
            return false;
        }

        return false;
    }


    /**
     * protostuff bytes deserialize by jprotobuf
     */
    public static boolean protostuffToJprotobuf() {

        // ProtoStuff serialize
        byte[] messageBytes = protostuffSerialize();

        try {
            // JProtobuf deserialize
            Student student = jprotobufDeserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }

        return false;
    }

    /**
     * jprotobuf bytes deserialize by protostuff
     */
    public static boolean jprotobufToProtostuff() {
        // jprotobuf serialize
        byte[] messageBytes = jprotobufSerialize();


        try {
            // protostuff deserialize
            Student student = protostuffDeserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }

        return false;
    }


    /**
     * protobuf bytes deserialize by jprotibuf (proto2)
     */
    public static boolean protobuf2ToJprotobuf() {
        // protobuf serialize
        byte[] messageBytes = protobuf2Serialize();

        try {
            Student student = jprotobufDeserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }

        } catch (IOException e) {
            System.out.println(e);
            return false;
        }

        return false;
    }

    /**
     * protobuf bytes deserialize by jprotibuf (proto3)
     */
    public static boolean protobuf3ToJprotobuf() {
        // protobuf serialize
        byte[] messageBytes = protobuf3Serialize();

        try {
            Student student = jprotobufDeserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }

        } catch (IOException e) {
            System.out.println(e);
            return false;
        }

        return false;
    }


    /**
     * protobuf bytes deserialize by protostuff (proto2)
     */
    public static boolean protobuf2ToProtostuff() {
        // protobuf serialize
        byte[] messageBytes = protobuf2Serialize();

        try {
            // protostuff deserialize
            Student student = protostuffDeserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return false;
    }

    /**
     * protobuf bytes deserialize by protostuff (proto3)
     */
    public static boolean protobuf3ToProtostuff() {
        // protobuf serialize
        byte[] messageBytes = protobuf3Serialize();

        try {
            // protostuff deserialize
            Student student = protostuffDeserialize(messageBytes);
            System.out.println(student);
            if (student.getDescription() != null && student.getDescription().contains("hello")) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return false;
    }


}
