package com.baidu.cloud.starlight.benchmark.serializer.protobuf.media;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.baidu.cloud.starlight.benchmark.model.Image;
import com.baidu.cloud.starlight.benchmark.model.Media;
import com.baidu.cloud.starlight.benchmark.model.MediaContent;
import com.baidu.cloud.starlight.benchmark.model.Player;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2019/11/5.
 */
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class JprotobufBenchmarkM {

    private static Codec codec = ProtobufProxy.create(MediaContent.class);

    static {
        ProtobufProxy.enableCache(true); // enabled schema cache
    }

    @State(Scope.Thread)
    public static class JprotoMediaMessage extends MediaMessage<MediaContent> {
        public JprotoMediaMessage() {
            super();
            byte[] messageBytes = new byte[0];
            try {
                messageBytes = codec.encode(this.getMediaContent());
            } catch (IOException e) {
                System.out.println("Error e :" + e);
            }
            this.setMessageBytes(messageBytes);
            System.out.println("Thread: " + Thread.currentThread().getName());
        }

        @Override
        public MediaContent createMediaContent() {
            MediaContent mediaContent = new MediaContent();
            Media media = new Media();
            mediaContent.setMedia(media);
            Image image1 = new Image();
            Image image2 = new Image();
            mediaContent.setImages(Arrays.asList(image1, image2));

            media.setUri("http://javaone.com/keynote.mpg");
            media.setTitle("Javaone Keynote");
            media.setWidth(640);
            media.setHeight(480);
            media.setFormat("video/mpg4");
            media.setDuration(18000000);
            media.setSize(58982400);
            media.setBitrate(262144);
            media.setPersons(Arrays.asList("Bill Gates", "Steve Jobs스"));
            media.setPlayer(Player.JAVA);

            image1.setUri("http://javaone.com/keynote_large.jpg");
            image1.setTitle("Javaone Keynote");
            image1.setWidth(1024);
            image1.setHeight(768);
            image1.setSize(Image.Size.LARGE);

            image2.setUri("http://javaone.com/keynote_small.jpg");
            image2.setTitle("Javaone Keynote");
            image2.setWidth(320);
            image2.setHeight(240);
            image2.setSize(Image.Size.SMALL);

            return mediaContent;
        }

    }


    @Benchmark
    public byte[] jprotobufSerilize(JprotoMediaMessage message) throws IOException {
        return codec.encode(message.createMediaContent());
    }

    @Benchmark
    public MediaContent jprotobufDeserilize(JprotoMediaMessage message) throws IOException {
        MediaContent mediaContent = (MediaContent) codec.decode(message.getMessageBytes());
        // System.out.println(mediaContent.toString());
        return mediaContent;
    }

    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(JprotobufBenchmarkM.class.getSimpleName())
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
