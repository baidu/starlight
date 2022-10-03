package com.baidu.cloud.starlight.benchmark.serializer.protobuf;

import com.baidu.cloud.starlight.benchmark.serializer.protobuf.JprotobufBenchmark;
import com.baidu.cloud.starlight.benchmark.serializer.protobuf.ProtobufBenchmark;
import com.google.protobuf.InvalidProtocolBufferException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/11/1.
 */

@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FramesBenchmark {

    private static final int THREAD_NUM = Runtime.getRuntime().availableProcessors();


    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    public static String messageTxt;

    private ProtobufBenchmark.ProtoStuMsg protoStuMsg;
    private Protobuf3Benchmark.Proto3StuMsg proto3StuMsg;
    private ProtoStuffBenchmark.ProtoStuffStuMsg protoStuffStuMsg;
    private JprotobufBenchmark.JProtoStuMsg jProtoStuMsg;


    private ProtobufBenchmark proto2 = new ProtobufBenchmark();
    private Protobuf3Benchmark proto3 = new Protobuf3Benchmark();
    private ProtoStuffBenchmark protoStuff = new ProtoStuffBenchmark();
    private JprotobufBenchmark jprotobuf = new JprotobufBenchmark();


    @Setup
    public void setUp() {
        protoStuMsg = new ProtobufBenchmark.ProtoStuMsg(messageTxt);
        proto3StuMsg = new Protobuf3Benchmark.Proto3StuMsg(messageTxt);
        protoStuffStuMsg = new ProtoStuffBenchmark.ProtoStuffStuMsg(messageTxt);
        jProtoStuMsg = new JprotobufBenchmark.JProtoStuMsg(messageTxt);
    }


    @Benchmark
    public void proto2Serialize() {
        // create and serialize
        proto2.protobuffSerialize(protoStuMsg);
    }

    @Benchmark
    public void proto2Deserialize() throws InvalidProtocolBufferException {
        proto2.protobuffDeseialize(protoStuMsg);
        // System.out.println(student2.toString());
    }


    @Benchmark
    public void proto3Serialize() {
        // create and serialize
        proto3.protobuffSerialize(proto3StuMsg);
    }

    @Benchmark
    public void proto3Deserialize() throws InvalidProtocolBufferException {
        proto3.protobuffDeseialize(proto3StuMsg);
    }

    @Benchmark
    public void protostuffSerialize() {
        protoStuff.protoStuffSerialize(protoStuffStuMsg);
    }

    @Benchmark
    public void protostuffDeserialize() {
        protoStuff.protoStuffDeserialize(protoStuffStuMsg);
    }

    @Benchmark
    public void jprotoSerialize() throws IOException {
        jprotobuf.jprotobufSerilize(jProtoStuMsg);
    }

    @Benchmark
    public void jprotoDeserialize() throws IOException {
        jprotobuf.jprotobufDeserilize(jProtoStuMsg);

    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(FramesBenchmark.class.getSimpleName())
                .forks(1)
                .result("result.json")
                .resultFormat(ResultFormatType.JSON)
                .threads(THREAD_NUM)
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            System.out.println("Run exception : " + e);
        }
    }


}
