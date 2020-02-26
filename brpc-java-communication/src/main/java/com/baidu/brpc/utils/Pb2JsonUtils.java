package com.baidu.brpc.utils;

import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.googlecode.protobuf.format.JsonFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class Pb2JsonUtils {
    public enum PbVersion {
        PROTO2,
        PROTO3
    }
    static {
        checkPbVersion();
    }

    private static PbVersion pbVersion;
    private static JsonFormat pb2Converter;
    private static Class pb3PrinterClazz;
    private static Object pb3Printer;
    private static Method pb3PrintMethod;
    private static Class pb3ParserClazz;
    private static Object pb3Parser;
    private static Method pb3ParseMethod;

    public static void checkPbVersion() {
        if (isClassExist("com.google.protobuf.MapField")) {
            pbVersion = PbVersion.PROTO3;
            try {
                Class jsonFormatClazz = Class.forName("com.google.protobuf.util.JsonFormat");
                Method method = jsonFormatClazz.getMethod("printer");
                pb3Printer = method.invoke(jsonFormatClazz);
                pb3PrinterClazz = Class.forName("com.google.protobuf.util.JsonFormat$Printer");
                method = pb3PrinterClazz.getDeclaredMethod("includingDefaultValueFields");
                method.invoke(pb3Printer);
                pb3PrintMethod = pb3PrinterClazz.getDeclaredMethod("print", MessageOrBuilder.class);

                method = jsonFormatClazz.getMethod("parser");
                pb3Parser = method.invoke(jsonFormatClazz);
                pb3ParserClazz = Class.forName("com.google.protobuf.util.JsonFormat$Parser");
                method = pb3ParserClazz.getDeclaredMethod("ignoringUnknownFields");
                method.invoke(pb3Parser);
                pb3ParseMethod = pb3ParserClazz.getDeclaredMethod("merge", String.class, Message.Builder.class);
            } catch (Exception ex) {
                throw new RuntimeException("dependency of protobuf-java-util not exist");
            }
        } else {
            pbVersion = PbVersion.PROTO2;
            pb2Converter = new JsonFormat() {
                protected void print(Message message, JsonGenerator generator) throws IOException {
                    for (Iterator<Map.Entry<Descriptors.FieldDescriptor, Object>> iter =
                         message.getAllFields().entrySet().iterator(); iter.hasNext(); ) {
                        Map.Entry<Descriptors.FieldDescriptor, Object> field = iter.next();
                        printField(field.getKey(), field.getValue(), generator);
                        if (iter.hasNext()) {
                            generator.print(",");
                        }
                    }
                    // ignore UnknownFields
                }
            };
        }
    }

    public static void json2Pb(CharSequence input, Message.Builder builder) throws JsonFormat.ParseException {
        if (pbVersion == PbVersion.PROTO2) {
            pb2Converter.merge(input, ExtensionRegistry.getEmptyRegistry(), builder);
        } else {
            try {
                pb3ParseMethod.invoke(pb3Parser, input, builder);
            } catch (Exception ex) {
                log.warn("pb3 json2pb failed, ex:", ex);
            }
        }
    }

    public static String pb2json(Message message, String encoding)
            throws IOException {
        if (pbVersion == pbVersion.PROTO2) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pb2Converter.print(message, out, Charset.forName(encoding));
            out.flush();
            return out.toString(encoding);
        } else {
            try {
                return (String) pb3PrintMethod.invoke(pb3Printer, message);
            } catch (Exception ex) {
                log.warn("pb3 pb2json failed, ex:", ex);
                return null;
            }
        }
    }

    public static boolean isClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
