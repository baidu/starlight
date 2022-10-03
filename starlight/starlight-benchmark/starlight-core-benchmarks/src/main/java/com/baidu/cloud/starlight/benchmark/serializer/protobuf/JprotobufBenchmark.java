package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.baidu.cloud.starlight.benchmark.model.School;
import com.baidu.cloud.starlight.benchmark.model.Student;
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/10/28.
 * protobuf是针对Java程序开发一套简易类库，目的是简化java语言对protobuf类库的使用
 * 参见https://github.com/jhunters/jprotobuf
 * 测试数据量：5byte 1k 2k 4k
 * JMH work线程数：1个
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class JprotobufBenchmark {

    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    private static Codec codec = ProtobufProxy.create(Student.class);

    static {
        ProtobufProxy.enableCache(true); // enabled schema cache
    }

    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    public static String messageTxt;

    @State(Scope.Thread)
    public static class JProtoStuMsg extends StudentMessage<Student> {

        // 加@State后会被线程初始化
        public JProtoStuMsg() {
            this(messageTxt);
        }


        public JProtoStuMsg(String messageTxt) {
            super(messageTxt);
            if (messageTxt != null && !messageTxt.isEmpty()) {

                byte[] messageBytes = new byte[0];
                try {
                    messageBytes = codec.encode(this.getStudent());
                } catch (IOException e) {
                    System.out.println("Error e :" + e);
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
    public byte[] jprotobufSerilize(JProtoStuMsg stuMsg) throws IOException {
        return codec.encode(stuMsg.createStudent(stuMsg.getDescription()));
    }

    @Benchmark
    public Student jprotobufDeserilize(JProtoStuMsg stuMsg) throws IOException {
        Student student2 = (Student) codec.decode(stuMsg.getMessageBytes());
        // System.out.println(student2.toString());
        return student2;
    }

    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(JprotobufBenchmark.class.getSimpleName())
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
