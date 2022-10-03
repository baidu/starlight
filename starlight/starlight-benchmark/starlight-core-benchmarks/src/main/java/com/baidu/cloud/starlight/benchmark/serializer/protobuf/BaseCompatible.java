package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.Student;
import com.baidu.cloud.starlight.benchmark.model.StudentProto;
import com.baidu.cloud.starlight.benchmark.model.StudentProto3;
import com.baidu.cloud.starlight.benchmark.serializer.jdk.JdkSerializeBenchmark;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;

/**
 * Created by liuruisen on 2019/10/31.
 */
public class BaseCompatible {

    private static final String MESSAGE_TXT = "/message.txt";

    public static byte[] jdkSerialize() {
        JdkSerializeBenchmark.JdkStuMsg stuMsg = new JdkSerializeBenchmark.JdkStuMsg(MESSAGE_TXT);

        if (stuMsg.getMessageBytes() != null) {
            System.out.println("JDK serialize size : " + stuMsg.getMessageBytes().length);
            return stuMsg.getMessageBytes();
        }
        return new byte[0];
    }


    protected static byte[] jprotobufSerialize() {
        JprotobufBenchmark.JProtoStuMsg jProtoStuMsg = new JprotobufBenchmark.JProtoStuMsg(MESSAGE_TXT);

        if (jProtoStuMsg.getMessageBytes() == null) {
            System.out.println("jprotobuf serialize fail, jprotobufToProtostuff fail");
            return new byte[0];
        }

        System.out.println("jprotobuf serialize size : " + jProtoStuMsg.getMessageBytes().length);

        return jProtoStuMsg.getMessageBytes();
    }


    protected static byte[] protobuf2Serialize() {
        // protobuf2 serialize
        ProtobufBenchmark.ProtoStuMsg protoStuMsg = new ProtobufBenchmark.ProtoStuMsg(MESSAGE_TXT);

        if (protoStuMsg.getMessageBytes() == null) {
            System.out.println("Protobuf2 serialize fail, protobufToJprotobuf fail");
            return new byte[0];
        }

        System.out.println("Protobuf2 serialize size : " + protoStuMsg.getMessageBytes().length);

        return protoStuMsg.getMessageBytes();
    }

    protected static byte[] protobuf3Serialize() {
        // protobuf3 serialize
        Protobuf3Benchmark.Proto3StuMsg proto3StuMsg = new Protobuf3Benchmark.Proto3StuMsg(MESSAGE_TXT);

        if (proto3StuMsg.getMessageBytes() == null) {
            System.out.println("Protobuf3 serialize fail, protobufToJprotobuf fail");
            return new byte[0];
        }

        System.out.println("Protobuf3 serialize size : " + proto3StuMsg.getMessageBytes().length);

        return proto3StuMsg.getMessageBytes();
    }

    protected static byte[] protostuffSerialize() {
        // ProtoStuff serialize
        ProtoStuffBenchmark.ProtoStuffStuMsg protoStuffStuMsg = new ProtoStuffBenchmark.ProtoStuffStuMsg(MESSAGE_TXT);

        if (protoStuffStuMsg.getMessageBytes() == null || protoStuffStuMsg.getMessageBytes().length <= 0) {
            System.out.println("protostuff serialize fail, protostuffToJprotobuf fail");
            return new byte[0];
        }

        System.out.println("protostuff serialize size : " + protoStuffStuMsg.getMessageBytes().length);

        return protoStuffStuMsg.getMessageBytes();
    }

    protected static Student jprotobufDeserialize(byte[] messageBytes) throws IOException {

        JprotobufBenchmark.JProtoStuMsg jProtoStuMsg = new JprotobufBenchmark.JProtoStuMsg();
        jProtoStuMsg.setMessageBytes(messageBytes);

        JprotobufBenchmark jprotobuf = new JprotobufBenchmark();
        Student student = jprotobuf.jprotobufDeserilize(jProtoStuMsg);

        return student;

    }

    protected static Student protostuffDeserialize(byte[] messageBytes) throws Exception {
        ProtoStuffBenchmark.ProtoStuffStuMsg protoStuffStuMsg = new ProtoStuffBenchmark.ProtoStuffStuMsg();
        protoStuffStuMsg.setMessageBytes(messageBytes);

        ProtoStuffBenchmark protoStuff = new ProtoStuffBenchmark();
        Student student = protoStuff.protoStuffDeserialize(protoStuffStuMsg);
        return student;
    }


    protected static StudentProto.Student protobuf2Deserialize(byte[] messageBytes)
            throws InvalidProtocolBufferException {
        ProtobufBenchmark.ProtoStuMsg protoStuMsg = new ProtobufBenchmark.ProtoStuMsg();
        protoStuMsg.setMessageBytes(messageBytes);

        // Protobuf deserialize
        ProtobufBenchmark protobuf = new ProtobufBenchmark();
        StudentProto.Student student = protobuf.protobuffDeseialize(protoStuMsg);
        return student;
    }


    protected static StudentProto3.Student protobuf3Deserialize(byte[] messageBytes)
            throws InvalidProtocolBufferException {
        Protobuf3Benchmark.Proto3StuMsg protoStuMsg = new Protobuf3Benchmark.Proto3StuMsg();
        protoStuMsg.setMessageBytes(messageBytes);

        // Protobuf deserialize
        Protobuf3Benchmark protobuf = new Protobuf3Benchmark();
        StudentProto3.Student student = protobuf.protobuffDeseialize(protoStuMsg);
        return student;
    }
}
