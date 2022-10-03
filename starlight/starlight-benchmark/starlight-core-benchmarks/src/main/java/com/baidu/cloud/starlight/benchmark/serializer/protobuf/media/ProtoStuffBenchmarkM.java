package com.baidu.cloud.starlight.benchmark.serializer.protobuf.media;

import com.baidu.cloud.starlight.benchmark.model.Image;
import com.baidu.cloud.starlight.benchmark.model.Media;
import com.baidu.cloud.starlight.benchmark.model.MediaContent;
import com.baidu.cloud.starlight.benchmark.model.Player;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
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
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ProtoStuffBenchmarkM {
    // Re-use (manage) this buffer to avoid allocating on every serialization
    private static final LinkedBuffer LINKED_BUFFER = LinkedBuffer.allocate(1024);
    private static Schema<MediaContent> schema = RuntimeSchema.getSchema(MediaContent.class);

    static {
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
        System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
    }

    @State(Scope.Thread)
    public static class StuffMediaMessage extends MediaMessage<MediaContent> {
        public StuffMediaMessage() {
            super();
            byte[] messageBytes;
            try {
                messageBytes = ProtobufIOUtil.toByteArray(this.getMediaContent(), schema, LINKED_BUFFER);
            } finally {
                LINKED_BUFFER.clear();
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

    // serialize
    @Benchmark
    public byte[] protoStuffSerialize(StuffMediaMessage message) {
        MediaContent mediaContent = message.createMediaContent();
        try {
            return ProtobufIOUtil.toByteArray(mediaContent, schema, LINKED_BUFFER);
        } finally {
            LINKED_BUFFER.clear();
        }
    }


    // deserialize
    @Benchmark
    public MediaContent protoStuffDeserialize(StuffMediaMessage message) {
        MediaContent content = new MediaContent();
        ProtobufIOUtil.mergeFrom(message.getMessageBytes(), content, schema);
        // System.out.println(content);
        return content;
    }


    @TearDown
    public void tearDown() {
        LINKED_BUFFER.clear();
        // System.out.println("Thread: " + Thread.currentThread().getName());
    }


    public static void main(String[] args) {

        Options options = new OptionsBuilder()
                .include(ProtoStuffBenchmarkM.class.getSimpleName())
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
