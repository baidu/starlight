package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.StudentProto;
import com.google.protobuf.InvalidProtocolBufferException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/10/28.
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ProtobufBenchmark {
    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    public static String messageTxt;


    @State(Scope.Thread)
    public static class ProtoStuMsg extends StudentMessage<StudentProto.Student> {
        // call by benchmark thread
        public ProtoStuMsg() {
            this(messageTxt);
        }

        public ProtoStuMsg(String messageTxt) {
            super(messageTxt);
            // set message bytes
            if (messageTxt != null && !messageTxt.isEmpty()) {
                this.setMessageBytes(this.getStudent().toByteArray());
                System.out.println("Thread: " + Thread.currentThread().getName() + " : " + messageTxt);
            }
        }

        @Override
        public StudentProto.Student createStudent(String description) {

            StudentProto.Experience experience = StudentProto.Experience.newBuilder()
                    .setInfo("born")
                    .setDate(new Date().toString())
                    .build();

            StudentProto.MapFieldEntry label = StudentProto.MapFieldEntry.newBuilder()
                    .setKey("Hobby").setValue("Study")
                    .build();

            StudentProto.School school = StudentProto.School.newBuilder()
                    .setName("Test").setAge(80).setLocation("Beijing")
                    .build();

            StudentProto.Student.Address address = StudentProto.Student.Address.newBuilder()
                    .setAddress("HaiDian").build();

            StudentProto.Student studentPoto = StudentProto.Student.newBuilder()
                    .setId(1234567899000011110l)
                    .setAge(20)
                    .setName("LiLei")
                    .setBalance(123.45f)
                    .setDescription(description)
                    .addExperiences(experience)
                    .addLabels(label)
                    .setSchool(school)
                    .setSex(StudentProto.Student.Sex.MALE)
                    .setScore(89.77777d)
                    .setAddress(address)
                    .setSuccess(true)
                    .build();

            return studentPoto;
        }
    }


    // create and serialize
    @Benchmark
    public byte[] protobuffSerialize(ProtoStuMsg stuMsg) {
        return stuMsg.createStudent(stuMsg.getDescription()).toByteArray();
    }


    // deserialize
    @Benchmark
    public StudentProto.Student protobuffDeseialize(ProtoStuMsg stuMsg) throws InvalidProtocolBufferException {
        StudentProto.Student student2 = StudentProto.Student.parseFrom(stuMsg.getMessageBytes());
        // System.out.println(student2.toString());
        return student2;
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(ProtobufBenchmark.class.getSimpleName())
                .forks(1)
                .threads(THREAD_NUM)
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            System.out.println("Run exception : " + e);
        }
    }
}
