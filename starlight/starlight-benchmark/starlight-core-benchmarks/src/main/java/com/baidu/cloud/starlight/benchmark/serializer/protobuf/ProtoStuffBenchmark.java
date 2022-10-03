package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.model.School;
import com.baidu.cloud.starlight.benchmark.model.Student;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/10/28.
 * ProtoStuff 的序列化和反序列化性能，
 * 测试数据量：5byte 1k 2k 4k
 * JMH work线程数：core num 个
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ProtoStuffBenchmark {
    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();

    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    public static String messageTxt;
    // Re-use (manage) this buffer to avoid allocating on every serialization
    private static final ThreadLocal<LinkedBuffer> BUFFER_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> LinkedBuffer.allocate());

    private static Schema<Student> schema = RuntimeSchema.getSchema(Student.class);

    static {
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
        System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
    }

    @State(Scope.Thread)
    public static class ProtoStuffStuMsg extends StudentMessage<Student> {

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

        // call by benchmark thread
        public ProtoStuffStuMsg() {
            this(messageTxt);
        }

        public ProtoStuffStuMsg(String messageTxt) {
            super(messageTxt);
            // set message bytes
            if (messageTxt != null && !messageTxt.isEmpty()) {
                LinkedBuffer buffer = BUFFER_THREAD_LOCAL.get();
                byte[] messageBytes;
                try {
                    messageBytes = ProtobufIOUtil.toByteArray(this.getStudent(), schema, buffer);
                } finally {
                    buffer.clear();
                }

                this.setMessageBytes(messageBytes);
                System.out.println("Thread: " + Thread.currentThread().getName() + " : " + messageTxt);
            }
        }
    }

    // create and serialize
    @Benchmark
    public byte[] protoStuffSerialize(ProtoStuffStuMsg studentMessage) {
        LinkedBuffer buffer = BUFFER_THREAD_LOCAL.get();
        try {
            // create student
            Student student = studentMessage.createStudent(studentMessage.getDescription());
            // serialize
            return ProtobufIOUtil.toByteArray(student, schema, buffer);
        } finally {
            buffer.clear();
        }
    }


    // deserialize
    @Benchmark
    public Student protoStuffDeserialize(ProtoStuffStuMsg studentMessage) {
        Student student2 = new Student();
        ProtobufIOUtil.mergeFrom(studentMessage.getMessageBytes(), student2, schema);
        // System.out.println(student2.toString());
        return student2;
    }


    @TearDown
    public void tearDown() {
        BUFFER_THREAD_LOCAL.remove();
        // System.out.println("Thread: " + Thread.currentThread().getName());
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(ProtoStuffBenchmark.class.getSimpleName())
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
