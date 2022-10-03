package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.School;
import com.baidu.cloud.starlight.benchmark.model.Student;
import com.baidu.cloud.starlight.benchmark.model.StudentProto3;
import com.baidu.cloud.starlight.benchmark.utils.BenchMarkUtil;

import java.io.IOException;

/**
 * Created by liuruisen on 2019/10/31.
 * 测试各种用户使用场景下的兼容性
 */
public class UpdateCompatible extends BaseCompatible {

    private static final String JPROTO_ADD_LAST_FILE = "/jproto_add_last_bytes";
    private static final String PROTO_ADD_LAST_FILE = "/proto_add_last_bytes";
    private static final String PROTOSTUFF_ADD_LAST_FILE = "/jprotostuff_add_last_bytes";


    private static final String JPROTO_ADD_MIDDLE_FILE = "/jproto_add_middle_bytes";
    private static final String PROTO_ADD_MIDDLE_FILE = "/proto_add_middle_bytes";
    private static final String PROTOSTUFF_ADD_MIDDLE_FILE = "/jprotostuff_add_middle_bytes";

    private static final String JPROTO_DELETE_MIDDLE_FILE = "/jproto_delete_middle_bytes";
    private static final String PROTO_DELETE_MIDDLE_FILE = "/proto_delete_middle_bytes";
    private static final String PROTOSTUFF_DELETE_MIDDLE_FILE = "/jprotostuff_delete_middle_bytes";

    private static final String JPROTO_EXCHANGE_FILE = "/jproto_exchange_bytes";
    private static final String PROTO_EXCHANGE_FILE = "/proto_exchange_bytes";
    private static final String PROTOSTUFF_EXCHANGE_FILE = "/jprotostuff_exchanget_bytes";

    public static void main(String[] args) {
        // addFieldAtLast();
        // addFieldAtMiddle();
        // deleteFieldAtMiddle();
        // exchangeField();
        jprotobufUpdateCompatible();
        protostuffUpdateCompatible();
        protobuf3UpdateCompatible();

    }


    private static void jprotobufUpdateCompatible() {

        // add last field
        try {
            Student student = jprotobufDeserialize(BenchMarkUtil.initInputMessageBytes(JPROTO_ADD_LAST_FILE));
            if (student != null && student.getSchool() != null) {
                System.out.println("Jprotobuf Deserialize Add Last Field Compatible: " + student.getSchool());
            } else {
                System.out.println("Jprotobuf Deserialize Add Last Field Compatible: " + false);
            }
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Jprotobuf Deserialize Add Last Field Compatible: " + false);
        }

        // add middle field
        try {
            Student student2 = jprotobufDeserialize(BenchMarkUtil.initInputMessageBytes(JPROTO_ADD_MIDDLE_FILE));
            if (student2 != null && student2.getSchool() != null) {
                System.out.println("Jprotobuf Deserialize Add Middle Field Compatible: " + student2.getSchool());
            } else {
                System.out.println("Jprotobuf Deserialize Add Middle Field Compatible: " + false);
            }
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Jprotobuf Deserialize Add Middle Field Compatible: " + false);
        }


        // delete middle field
        try {
            Student student3 = jprotobufDeserialize(BenchMarkUtil.initInputMessageBytes(JPROTO_DELETE_MIDDLE_FILE));
            if (student3 != null && student3.getSchool() != null) {
                System.out.println("Jprotobuf Deserialize Delete Middle Field Compatible: " + student3.getSchool());
            } else {
                System.out.println("Jprotobuf Deserialize Delete Middle Field Compatible: " + false);
            }
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Jprotobuf Deserialize Delete Middle Field Compatible: " + false);
        }

        // exchange field
        try {
            Student student4 = jprotobufDeserialize(BenchMarkUtil.initInputMessageBytes(JPROTO_EXCHANGE_FILE));
            if (student4 != null && student4.getSchool() != null) {
                System.out.println("Jprotobuf Deserialize Exchange Field Compatible: " + student4.getSchool());
            } else {
                System.out.println("Jprotobuf Deserialize Exchange Field Compatible: " + false);
            }
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Jprotobuf Deserialize Exchange Field Compatible: " + false);
        }

    }


    private static void protostuffUpdateCompatible() {

        // add last field
        try {
            Student student = protostuffDeserialize(BenchMarkUtil.initInputMessageBytes(PROTOSTUFF_ADD_LAST_FILE));
            if (student != null && student.getSchool() != null) {
                System.out.println("ProtoStuff Deserialize Add Last Field Compatible: " + student.getSchool());
            } else {
                System.out.println("ProtoStuff Deserialize Add Last Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("ProtoStuff Deserialize Add Last Field Compatible: " + false);
        }

        // add middle field
        try {
            Student student2 = protostuffDeserialize(BenchMarkUtil.initInputMessageBytes(PROTOSTUFF_ADD_MIDDLE_FILE));
            if (student2 != null && student2.getSchool() != null) {
                System.out.println("ProtoStuff Deserialize Add Middle Field Compatible: " + student2.getSchool());
            } else {
                System.out.println("ProtoStuff Deserialize Add Middle Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("ProtoStuff Deserialize Add Middle Field Compatible: " + false);
        }


        // delete middle field
        try {
            Student student3 = protostuffDeserialize(BenchMarkUtil.initInputMessageBytes(PROTOSTUFF_DELETE_MIDDLE_FILE));
            if (student3 != null && student3.getSchool() != null) {
                System.out.println("ProtoStuff Deserialize Delete Middle Field Compatible: " + student3.getSchool());
            } else {
                System.out.println("ProtoStuff Deserialize Delete Middle Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("ProtoStuff Deserialize Delete Middle Field Compatible: " + false);
        }

        // exchange field
        try {
            Student student4 = protostuffDeserialize(BenchMarkUtil.initInputMessageBytes(PROTOSTUFF_EXCHANGE_FILE));
            if (student4 != null && student4.getSchool() != null) {
                System.out.println("ProtoStuff Deserialize Exchange Field Compatible: " + student4.getSchool());
            } else {
                System.out.println("ProtoStuff Deserialize Exchange Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("ProtoStuff Deserialize Exchange Field Compatible: " + false);
        }

    }


    private static void protobuf3UpdateCompatible() {

        // add last field
        try {
            StudentProto3.Student student = protobuf3Deserialize(BenchMarkUtil.initInputMessageBytes(PROTO_ADD_LAST_FILE));
            if (student != null && student.getSchool() != null) {
                School school = new School();
                school.setLocation(student.getSchool().getLocation());
                school.setAge(student.getSchool().getAge());
                school.setName(student.getSchool().getName());
                System.out.println("Proto3 Deserialize Add Last Field Compatible: " + school);
            } else {
                System.out.println("Proto3 Deserialize Add Last Field Compatible: " + false);
            }
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Proto3 Deserialize Add Last Field Compatible: " + false);
        }

        // add middle field
        try {
            StudentProto3.Student student2 = protobuf3Deserialize(BenchMarkUtil.initInputMessageBytes(PROTO_ADD_MIDDLE_FILE));
            if (student2 != null && student2.getSchool() != null) {
                School school = new School();
                school.setLocation(student2.getSchool().getLocation());
                school.setAge(student2.getSchool().getAge());
                school.setName(student2.getSchool().getName());
                System.out.println("Proto3 Deserialize Add Middle Field Compatible: " + school);
            } else {
                System.out.println("Proto3 Deserialize Add Middle Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Proto3 Deserialize Add Middle Field Compatible: " + false);
        }


        // delete middle field
        try {
            StudentProto3.Student student3 = protobuf3Deserialize(BenchMarkUtil.initInputMessageBytes(PROTO_DELETE_MIDDLE_FILE));
            if (student3 != null && student3.getSchool() != null) {
                School school = new School();
                school.setLocation(student3.getSchool().getLocation());
                school.setAge(student3.getSchool().getAge());
                school.setName(student3.getSchool().getName());
                System.out.println("Proto3 Deserialize Delete Middle Field Compatible: " + school);
            } else {
                System.out.println("Proto3 Deserialize Delete Middle Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Proto3 Deserialize Delete Middle Field Compatible: " + false);
        }

        // exchange field
        try {
            StudentProto3.Student student4 = protobuf3Deserialize(BenchMarkUtil.initInputMessageBytes(PROTO_EXCHANGE_FILE));
            if (student4 != null && student4.getSchool() != null) {
                School school = new School();
                school.setLocation(student4.getSchool().getLocation());
                school.setAge(student4.getSchool().getAge());
                school.setName(student4.getSchool().getName());
                System.out.println("Proto3 Deserialize Exchange Field Compatible: " + school);
            } else {
                System.out.println("Proto3 Deserialize Exchange Field Compatible: " + false);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Proto3 Deserialize Exchange Field Compatible: " + false);
        }

    }



    /**
     * School Class add "String addField" at last
     */
    public static void addFieldAtLast() {

        byte[] messageBytes = jprotobufSerialize();
        BenchMarkUtil.writeIntoFile(JPROTO_ADD_LAST_FILE, messageBytes);

        byte[] msgBytes2 = protostuffSerialize();
        BenchMarkUtil.writeIntoFile(PROTOSTUFF_ADD_LAST_FILE, msgBytes2);

        byte[] msgBytes3 = protobuf3Serialize();
        BenchMarkUtil.writeIntoFile(PROTO_ADD_LAST_FILE, msgBytes3);

    }

    /**
     * School Class add "String addField" at Middle
     */
    public static void addFieldAtMiddle() {

        byte[] messageBytes = jprotobufSerialize();
        BenchMarkUtil.writeIntoFile(JPROTO_ADD_MIDDLE_FILE, messageBytes);

        byte[] msgBytes2 = protostuffSerialize();
        BenchMarkUtil.writeIntoFile(PROTOSTUFF_ADD_MIDDLE_FILE, msgBytes2);

        byte[] msgBytes3 = protobuf3Serialize();
        BenchMarkUtil.writeIntoFile(PROTO_ADD_MIDDLE_FILE, msgBytes3);

    }


    /**
     * School Class  delete Field at Middle: age
     */
    public static void deleteFieldAtMiddle() {

        byte[] messageBytes = jprotobufSerialize();
        BenchMarkUtil.writeIntoFile(JPROTO_DELETE_MIDDLE_FILE, messageBytes);

        byte[] msgBytes2 = protostuffSerialize();
        BenchMarkUtil.writeIntoFile(PROTOSTUFF_DELETE_MIDDLE_FILE, msgBytes2);

        byte[] msgBytes3 = protobuf3Serialize();
        BenchMarkUtil.writeIntoFile(PROTO_DELETE_MIDDLE_FILE, msgBytes3);
    }


    /**
     * School Class  delete Field at Middle: age location
     */
    public static void exchangeField() {

        byte[] messageBytes = jprotobufSerialize();
        BenchMarkUtil.writeIntoFile(JPROTO_EXCHANGE_FILE, messageBytes);

        byte[] msgBytes2 = protostuffSerialize();
        BenchMarkUtil.writeIntoFile(PROTOSTUFF_EXCHANGE_FILE, msgBytes2);

        byte[] msgBytes3 = protobuf3Serialize();
        BenchMarkUtil.writeIntoFile(PROTO_EXCHANGE_FILE, msgBytes3);

    }

}
