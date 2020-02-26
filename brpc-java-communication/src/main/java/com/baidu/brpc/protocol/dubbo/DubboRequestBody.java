package com.baidu.brpc.protocol.dubbo;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Slf4j
public class DubboRequestBody {
    /**
     * It is important to share the SerializerFactory instance for all Hessian2Input and HessianOutput instances!
     */
    public static final SerializerFactory SERIALIZER_FACTORY = new SerializerFactory();
    private String dubboProtocolVersion = DubboConstants.DEFAULT_DUBBO_PROTOCOL_VERSION;
    private String path; // service name
    private String version; // version of service
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] arguments;
    private Map<String, String> attachments = new HashMap<String, String>();
    private RpcMethodInfo rpcMethodInfo; // not serialize

    public byte[] encodeRequestBody() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.setSerializerFactory(SERIALIZER_FACTORY);
        hessian2Output.writeString(dubboProtocolVersion);
        hessian2Output.writeString(path);
        hessian2Output.writeString(version);
        hessian2Output.writeString(methodName);
        hessian2Output.writeString(ReflectUtils.getDesc(parameterTypes));
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                hessian2Output.writeObject(arguments[i]);
            }
        }
        if (attachments != null) {
            hessian2Output.writeObject(attachments);
        }
        hessian2Output.flushBuffer();
        return outputStream.toByteArray();
    }

    public static DubboRequestBody decodeRequestBody(ByteBuf requestBodyBuf) throws IOException {
        ByteBufInputStream inputStream = null;
        try {
            inputStream = new ByteBufInputStream(requestBodyBuf, true);
            Hessian2Input hessian2Input = new Hessian2Input(inputStream);
            hessian2Input.setSerializerFactory(SERIALIZER_FACTORY);
            DubboRequestBody requestBody = new DubboRequestBody();
            requestBody.setDubboProtocolVersion(hessian2Input.readString());
            requestBody.setPath(hessian2Input.readString());
            requestBody.setVersion(hessian2Input.readString());
            requestBody.setMethodName(hessian2Input.readString());

            String serviceName = requestBody.getPath();
            String methodName = requestBody.getMethodName();
            RpcMethodInfo rpcMethodInfo = ServiceManager.getInstance().getService(serviceName, methodName);
            if (rpcMethodInfo == null) {
                throw new RpcException(RpcException.SERVICE_EXCEPTION,
                        "service not found, serviceName:" + serviceName + ", methodName:{}" + methodName);
            }
            requestBody.setRpcMethodInfo(rpcMethodInfo);
            Type[] inputTypes = rpcMethodInfo.getInputClasses();

            Object[] args;
            Class<?>[] pts;
            String desc = hessian2Input.readString();
            if (desc.length() == 0) {
                pts = new Class<?>[0];
                args = new Object[0];
            } else {
                // 优化反射
//                pts = ReflectUtils.desc2classArray(desc);
                pts = new Class[inputTypes.length];
                args = new Object[inputTypes.length];
                for (int i = 0; i < args.length; i++) {
                    try {
                        pts[i] = (Class<?>) inputTypes[i];
                        args[i] = hessian2Input.readObject(pts[i]);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Decode argument failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
            requestBody.setParameterTypes(pts);
            requestBody.setArguments(args);

            Map<String, String> map = (Map<String, String>) hessian2Input.readObject(Map.class);
            if (map != null && map.size() > 0) {
                requestBody.getAttachments().putAll(map);
            }
            return requestBody;
        } catch (Exception e) {
            throw new IOException("Read invocation data failed.", e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
