package com.baidu.cloud.starlight.benchmark.serializer.jdk;

import com.baidu.cloud.starlight.benchmark.model.School;
import com.baidu.cloud.starlight.benchmark.model.Student;
import com.baidu.cloud.starlight.benchmark.serializer.protobuf.StudentMessage;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/10/31.
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class JdkSerializeBenchmark {
    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    public static String messageTxt;


    @State(Scope.Thread)
    public static class JdkStuMsg extends StudentMessage<Student> {

        public JdkStuMsg() {
            this(messageTxt);
        }

        public JdkStuMsg(String messageTxt) {
            super(messageTxt);

            if (messageTxt != null && !messageTxt.isEmpty()) {

                byte[] messageBytes = new byte[0];
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                    oos.writeObject(this.getStudent());
                    oos.flush();
                    messageBytes = baos.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.setMessageBytes(messageBytes);
                System.out.println("Thread: " + Thread.currentThread().getName() + " : " + messageTxt);
            }
        }

        @Override
        public Student createStudent(String description) {
            Student student = new Student("LiLei");
            student.setDescription(description);
            School school = student.getSchool();
            school.setName("Test");
            school.setAge(80);
            school.setLocation("Beijing");
            return student;
        }
    }


    @Benchmark
    public byte[] bodySerialize(JdkStuMsg jdkStuMsg) throws IOException {
        byte[] bytes = null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(jdkStuMsg.getStudent());
            oos.flush();
            bytes = baos.toByteArray();

        }
        return bytes;
    }

    @Benchmark
    public Student bodyDeserialize(JdkStuMsg jdkStuMsg) throws Exception {
        Student student = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(jdkStuMsg.getMessageBytes());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            student = (jdkStuMsg.getStudent().getClass()).cast(ois.readObject());
            // System.out.println(student);
        }
        return student;
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(JdkSerializeBenchmark.class.getSimpleName())
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
