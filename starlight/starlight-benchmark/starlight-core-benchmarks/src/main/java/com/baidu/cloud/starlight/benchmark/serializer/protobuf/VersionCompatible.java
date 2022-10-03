package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.StudentProto;
import com.baidu.cloud.starlight.benchmark.model.StudentProto3;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Created by liuruisen on 2019/10/31.
 * 测试兼容性
 * - proto2 与 proto3 兼容性
 */
public class VersionCompatible extends BaseCompatible {


    public static void main(String[] args) {
        System.out.println("-----Proto2 To Proto3 : " + proto2ToProto3());
        System.out.println("-----Proto3 To Proto2 : " + proto3ToProto2());
    }


    public static boolean proto2ToProto3() {

        byte[] messageBytes = protobuf2Serialize();

        try {
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


    public static boolean proto3ToProto2() {

        byte[] messageBytes = protobuf3Serialize();

        try {
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

}
