package com.baidu.cloud.starlight.benchmark.serializer.protobuf.media;

import com.baidu.cloud.starlight.benchmark.model.MediaContentHolder3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;

/**
 * Created by liuruisen on 2019/11/5.
 */
public class Protobuf3BenchmarkM {

    @State(Scope.Thread)
    public static class Proto3MediaMessage extends MediaMessage<MediaContentHolder3.MediaContent> {


        public Proto3MediaMessage() {
            super();
            this.setMessageBytes(this.getMediaContent().toByteArray());
            System.out.println("Thread: " + Thread.currentThread().getName());
        }

        @Override
        public MediaContentHolder3.MediaContent createMediaContent() {
            MediaContentHolder3.Media media = MediaContentHolder3.Media.newBuilder()
                    .setUri("http://javaone.com/keynote.mpg")
                    .setTitle("Javaone Keynote")
                    .setWidth(640)
                    .setHeight(480)
                    .setFormat("video/mpg4")
                    .setDuration(18000000)
                    .setSize(58982400)
                    .setBitrate(262144)
                    .addAllPerson(Arrays.asList("Bill Gates", "Steve Jobs스"))
                    .setPlayer(MediaContentHolder3.Media.Player.JAVA)
                    .build();


            MediaContentHolder3.Image image = MediaContentHolder3.Image.newBuilder()
                    .setUri("http://javaone.com/keynote_large.jpg")
                    .setTitle("Javaone Keynote")
                    .setWidth(1024)
                    .setHeight(768)
                    .setSize(MediaContentHolder3.Image.Size.LARGE)
                    .build();

            MediaContentHolder3.Image image1 = MediaContentHolder3.Image.newBuilder()
                    .setUri("http://javaone.com/keynote_small.jpg")
                    .setTitle("Javaone Keynote")
                    .setWidth(320)
                    .setHeight(240)
                    .setSize(MediaContentHolder3.Image.Size.SMALL)
                    .build();


            MediaContentHolder3.MediaContent mediaContent = MediaContentHolder3.MediaContent.newBuilder()
                    .setMedia(media)
                    .addImage(0, image)
                    .addImage(1, image1)
                    .build();

            return mediaContent;
        }

    }


    @Benchmark
    public byte[] protobuffSerialize(Proto3MediaMessage message) {
        MediaContentHolder3.MediaContent mediaContent = message.createMediaContent();

        return mediaContent.toByteArray();
    }


    @Benchmark
    public MediaContentHolder3.MediaContent protobuffDeseialize(Proto3MediaMessage message)
            throws InvalidProtocolBufferException {
        MediaContentHolder3.MediaContent mediaContent = MediaContentHolder3.MediaContent.parseFrom(message.getMessageBytes());
        // System.out.println(mediaContent.toString());
        return mediaContent;
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(Protobuf3BenchmarkM.class.getSimpleName())
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
