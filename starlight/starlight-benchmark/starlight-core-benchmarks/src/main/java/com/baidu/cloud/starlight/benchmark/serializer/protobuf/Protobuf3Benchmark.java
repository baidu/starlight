package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.StudentProto3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
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

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/10/31.
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class Protobuf3Benchmark {

    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    public static String messageTxt;

    @State(Scope.Thread)
    public static class Proto3StuMsg extends StudentMessage<StudentProto3.Student> {

        public Proto3StuMsg() {
            this(messageTxt);
        }

        public Proto3StuMsg(String messageTxt) {
            super(messageTxt);
            // set message bytes: serialize
            if (messageTxt != null && !messageTxt.isEmpty()) {
                this.setMessageBytes(this.getStudent().toByteArray());
                System.out.println("Thread: " + Thread.currentThread().getName() + " : " + messageTxt);
            }

        }

        @Override
        public StudentProto3.Student createStudent(String description) {
            StudentProto3.Experience experience = StudentProto3.Experience.newBuilder()
                    .setInfo("born")
                    .setDate(new Date().toString())
                    .build();

            StudentProto3.MapFieldEntry label = StudentProto3.MapFieldEntry.newBuilder()
                    .setKey("Hobby").setValue("Study")
                    .build();

            StudentProto3.School school = StudentProto3.School.newBuilder()
                    .setName("Test")
                    .setAge(80)
                    .setLocation("Beijing")
                    .build();

            StudentProto3.Student.Address address = StudentProto3.Student.Address.newBuilder()
                    .setAddress("HaiDian").build();

            Instant time = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
                    .setNanos(time.getNano()).build();

            StudentProto3.Student studentPoto = StudentProto3.Student.newBuilder()
                    .setId(1234567899000011110l)
                    .setAge(20)
                    .setName("LiLei")
                    .setBalance(123.45f)
                    .setDescription(description)
                    .addExperiences(experience)
                    .putLabels("Hobby", "Study")
                    .setSchool(school)
                    .setSex(StudentProto3.Student.Sex.MALE)
                    .setScore(89.77777d)
                    .setAddress(address)
                    .setSuccess(true)
                    // .setDate(timestamp)
                    .build();

            return studentPoto;
        }
    }


    @Benchmark
    public byte[] protobuffSerialize(Protobuf3Benchmark.Proto3StuMsg stuMsg) {
        return stuMsg.createStudent(stuMsg.getDescription()).toByteArray();
    }


    @Benchmark
    public StudentProto3.Student protobuffDeseialize(Protobuf3Benchmark.Proto3StuMsg stuMsg)
            throws InvalidProtocolBufferException {
        StudentProto3.Student student2 = StudentProto3.Student.parseFrom(stuMsg.getMessageBytes());
        // System.out.println(student2.toString());
        return student2;
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(Protobuf3Benchmark.class.getSimpleName())
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
