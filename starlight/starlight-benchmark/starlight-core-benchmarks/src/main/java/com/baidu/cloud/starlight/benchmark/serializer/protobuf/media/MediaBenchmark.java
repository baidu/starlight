package com.baidu.cloud.starlight.benchmark.serializer.protobuf.media;

import com.google.protobuf.InvalidProtocolBufferException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
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
 * Created by liuruisen on 2019/11/4.
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MediaBenchmark {


    ProtobufBenchmarkM.ProtoMediaMessage protoMessage = new ProtobufBenchmarkM.ProtoMediaMessage();
    Protobuf3BenchmarkM.Proto3MediaMessage proto3Message = new Protobuf3BenchmarkM.Proto3MediaMessage();
    ProtoStuffBenchmarkM.StuffMediaMessage stufMessgae = new ProtoStuffBenchmarkM.StuffMediaMessage();
    JprotobufBenchmarkM.JprotoMediaMessage jprotoMessage = new JprotobufBenchmarkM.JprotoMediaMessage();

    ProtobufBenchmarkM proto = new ProtobufBenchmarkM();
    Protobuf3BenchmarkM proto3 = new Protobuf3BenchmarkM();
    ProtoStuffBenchmarkM stuff = new ProtoStuffBenchmarkM();
    JprotobufBenchmarkM jproto = new JprotobufBenchmarkM();



    @Benchmark
    public void proto2Serialize() {
        proto.protobuffSerialize(protoMessage);
    }

    @Benchmark
    public void proto2Deserialize() throws InvalidProtocolBufferException {
        proto.protobuffDeseialize(protoMessage);
    }

    @Benchmark
    public void proto3Serialize() {
        proto3.protobuffSerialize(proto3Message);
    }

    @Benchmark
    public void proto3Deserialize() throws InvalidProtocolBufferException {
        proto3.protobuffDeseialize(proto3Message);
    }

    @Benchmark
    public void protostuffSerialize() {
        stuff.protoStuffSerialize(stufMessgae);
    }

    @Benchmark
    public void protostuffDeserialize() {
        stuff.protoStuffDeserialize(stufMessgae);
    }

    @Benchmark
    public void jprotoSerialize() throws IOException {
        jproto.jprotobufSerilize(jprotoMessage);
    }

    @Benchmark
    public void jprotoDeserialize() throws IOException {
        jproto.jprotobufDeserilize(jprotoMessage);
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(MediaBenchmark.class.getSimpleName())
                .forks(1)
                .result("result.json")
                .resultFormat(ResultFormatType.JSON)
                .threads(1)
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            System.out.println("Run exception : " + e);
        }
    }
}
