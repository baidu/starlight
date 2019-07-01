package com.baidu.brpc.client;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import com.baidu.brpc.JprotobufRpcMethodInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.utils.ProtobufUtils;

public class MethodUtils {

    public static RpcMethodInfo getRpcMethodInfo(Class clazz, String methodName) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class[] parameterTypes = method.getParameterTypes();
            int paramLength = parameterTypes.length;
            if (paramLength < 1) {
                throw new IllegalArgumentException(
                        "invalid params, the correct is ([RpcContext], Request, [Callback])");
            }
            if (Future.class.isAssignableFrom(method.getReturnType())
                    && (paramLength < 1 || !RpcCallback.class.isAssignableFrom(parameterTypes[paramLength - 1]))) {
                throw new IllegalArgumentException("returnType is Future, but last argument is not RpcCallback");
            }

            Method syncMethod = method;
            if (paramLength > 1) {
                int startIndex = 0;
                int endIndex = paramLength - 1;
                // has callback, async rpc
                if (RpcCallback.class.isAssignableFrom(parameterTypes[paramLength - 1])) {
                    endIndex--;
                    paramLength--;
                }
                Class[] actualParameterTypes = new Class[paramLength];
                for (int i = 0; startIndex <= endIndex; i++) {
                    actualParameterTypes[i] = parameterTypes[startIndex++];
                }
                try {
                    syncMethod = method.getDeclaringClass().getMethod(
                            method.getName(), actualParameterTypes);
                } catch (NoSuchMethodException ex) {
                    throw new IllegalArgumentException("can not find sync method:" + method.getName());
                }
            }

            RpcMethodInfo methodInfo;
            ProtobufUtils.MessageType messageType = ProtobufUtils.getMessageType(syncMethod);
            if (messageType == ProtobufUtils.MessageType.PROTOBUF) {
                methodInfo = new ProtobufRpcMethodInfo(syncMethod);
            } else if (messageType == ProtobufUtils.MessageType.JPROTOBUF) {
                methodInfo = new JprotobufRpcMethodInfo(syncMethod);
            } else {
                methodInfo = new RpcMethodInfo(syncMethod);
            }

            return methodInfo;
        }
        return null;
    }
}
