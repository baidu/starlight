package com.baidu.cloud.starlight.benchmark.serializer.protobuf.media;

import com.baidu.cloud.starlight.benchmark.model.MediaContentHolder;
import com.google.protobuf.InvalidProtocolBufferException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;


/**
 * Created by liuruisen on 2019/11/4.
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ProtobufBenchmarkM {

    @State(Scope.Thread)
    public static class ProtoMediaMessage extends MediaMessage<MediaContentHolder.MediaContent> {


        public ProtoMediaMessage() {
            super();
            this.setMessageBytes(this.getMediaContent().toByteArray());
            System.out.println("Thread: " + Thread.currentThread().getName());
        }

        @Override
        public MediaContentHolder.MediaContent createMediaContent() {
            MediaContentHolder.Media media = MediaContentHolder.Media.newBuilder()
                    .setUri("http://javaone.com/keynote.mpg")
                    .setTitle("Javaone Keynote")
                    .setWidth(640)
                    .setHeight(480)
                    .setFormat("video/mpg4")
                    .setDuration(18000000)
                    .setSize(58982400)
                    .setBitrate(262144)
                    .addAllPerson(Arrays.asList("Bill Gates", "Steve Jobs스"))
                    .setPlayer(MediaContentHolder.Media.Player.JAVA)
                    .build();


            MediaContentHolder.Image image = MediaContentHolder.Image.newBuilder()
                    .setUri("http://javaone.com/keynote_large.jpg")
                    .setTitle("Javaone Keynote")
                    .setWidth(1024)
                    .setHeight(768)
                    .setSize(MediaContentHolder.Image.Size.LARGE)
                    .build();

            MediaContentHolder.Image image1 = MediaContentHolder.Image.newBuilder()
                    .setUri("http://javaone.com/keynote_small.jpg")
                    .setTitle("Javaone Keynote")
                    .setWidth(320)
                    .setHeight(240)
                    .setSize(MediaContentHolder.Image.Size.SMALL)
                    .build();


            MediaContentHolder.MediaContent mediaContent = MediaContentHolder.MediaContent.newBuilder()
                    .setMedia(media)
                    .addImage(0, image)
                    .addImage(1, image1)
                    .build();

            return mediaContent;
        }

    }


    @Benchmark
    public byte[] protobuffSerialize(ProtoMediaMessage message) {
        MediaContentHolder.MediaContent mediaContent = message.createMediaContent();

        return mediaContent.toByteArray();
    }


    @Benchmark
    public MediaContentHolder.MediaContent protobuffDeseialize(ProtoMediaMessage message)
            throws InvalidProtocolBufferException {
        MediaContentHolder.MediaContent mediaContent = MediaContentHolder.MediaContent.parseFrom(message.getMessageBytes());
        // System.out.println(mediaContent.toString());
        return mediaContent;
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(ProtobufBenchmarkM.class.getSimpleName())
                .forks(1)
                .threads(1)
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            System.out.println("Run exception : " + e);
        }
    }
}
