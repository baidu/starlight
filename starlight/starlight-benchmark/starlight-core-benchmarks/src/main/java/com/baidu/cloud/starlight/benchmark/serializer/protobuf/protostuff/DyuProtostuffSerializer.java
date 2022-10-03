package com.baidu.cloud.starlight.benchmark.serializer.protobuf.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.IdStrategy;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by liuruisen on 2020/9/9.
 */
public class DyuProtostuffSerializer {

    private static final int DEFAULT_ALLOCATE_NUM = 512;

    // Re-use (manage) this buffer to avoid allocating on every serialization
    private static LinkedBuffer buffer = LinkedBuffer.allocate(DEFAULT_ALLOCATE_NUM);

    private static final Map<Class<?>, Schema> customSchemaCache = new ConcurrentHashMap<>();

    /**
     * bodySerialize
     *
     * @param body
     * @param inputType
     * @return
     * @throws Exception
     */
    public static byte[] bodySerialize(Object body, Type inputType) throws Exception {
        // use predefine object container as default
        Schema schema = RuntimeSchema.getSchema((Class) inputType); // cache inner
        System.out.println("BodySerialize schema: " + schema);
        try {
            return ProtobufIOUtil.toByteArray(body, schema, buffer);
        } catch (Exception e) {
            throw e;
        } finally {
            buffer.clear();
        }

    }

    public static Object bodyDeserialize(byte[] output, Type outType) throws Exception {
        // use predefine object container as default
        Schema schema = RuntimeSchema.getSchema((Class) outType); // cached inner
        try {
            Object content = schema.newMessage();
            ProtobufIOUtil.mergeFrom(output, content, schema);
            return content;
        } catch (Exception e) {
            throw e;
        }
    }

    public static byte[] bodySerialize(Object body, Type inputType, IdStrategy idStrategy) throws Exception {
        // use predefine object container as default
        // Schema schema = RuntimeSchema.getSchema((Class) inputType); // cache inner
        // Schema schema = RuntimeSchema.createFrom((Class) inputType, new DefaultIdStrategy(idStrategyFlags));
        Schema schema = RuntimeSchema.getSchema((Class) inputType, idStrategy); // cache inner
        System.out.println("BodySerialize idStrategyFlags schema: " + schema);
        try {
            return ProtobufIOUtil.toByteArray(body, schema, buffer);
        } catch (Exception e) {
            throw e;
        } finally {
            buffer.clear();
        }

    }

    public static Object bodyDeserialize(byte[] output, Type outType, IdStrategy idStrategy) throws Exception {
        // use predefine object container as default
        // Schema schema = RuntimeSchema.createFrom((Class) outType, new DefaultIdStrategy(idStrategyFlags));
        Schema schema = RuntimeSchema.getSchema((Class) outType, idStrategy);
        try {
            Object content = schema.newMessage();
            ProtobufIOUtil.mergeFrom(output, content, schema);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
