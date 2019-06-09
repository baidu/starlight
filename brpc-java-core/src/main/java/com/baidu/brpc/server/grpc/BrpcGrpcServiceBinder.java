package com.baidu.brpc.server.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.Method;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;


/**
 * bind brpc service to grpc server
 * @author keweiwang@gmail.com
 * @param <ReqT>
 * @param <RespT>
 */
public class BrpcGrpcServiceBinder<ReqT extends GeneratedMessage, RespT extends GeneratedMessage> {

    private Object serviceInstance;

    private String fullServiceName;

    private String methodName;

    private Descriptors.FileDescriptor protoFileDescriptor;

    private ReqT requestDefaultInstance;
    private RespT responseDefaultInstance;

    private Class<ReqT> requestDefaultClazz;

    private Class serviceClazz;

    private volatile io.grpc.MethodDescriptor<ReqT, RespT> getGrpcMethodInstance;

    private volatile io.grpc.ServiceDescriptor getGrpcServiceDescriptorInstance;


    public BrpcGrpcServiceBinder(Class serviceClazz, Class requestDefaultClazz, Descriptors.FileDescriptor protoFileDescriptor, String fullServiceName, String methodName, ReqT requestDefaultInstance, RespT responseDefaultInstance) throws IllegalAccessException, InstantiationException {
        this.fullServiceName = fullServiceName;
        this.methodName = methodName;
        this.requestDefaultInstance = requestDefaultInstance;
        this.responseDefaultInstance = responseDefaultInstance;
        this.protoFileDescriptor = protoFileDescriptor;
        this.serviceClazz = serviceClazz;
        this.serviceInstance = serviceClazz.newInstance();
        this.requestDefaultClazz = requestDefaultClazz;
    }


    private MethodDescriptor getGrpcMethod() {
        MethodDescriptor<ReqT, RespT> getGrpcMethodResult;
        if ((getGrpcMethodResult = getGrpcMethodInstance) == null) {
            synchronized (BrpcGrpcServiceBinder.class) {
                if ((getGrpcMethodResult = getGrpcMethodInstance) == null) {
                    getGrpcMethodResult = getGrpcMethodInstance =
                            MethodDescriptor.<ReqT, RespT>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            fullServiceName, methodName))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(requestDefaultInstance))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(responseDefaultInstance))
                                    .setSchemaDescriptor(new GrpcServiceMethodDescriptorSupplier(methodName))
                                    .build();
                }

            }
        }
        return getGrpcMethodResult;

    }

    public final ServerServiceDefinition bindService(String methodName) {
        return ServerServiceDefinition.builder(getServiceDescriptor())
                .addMethod(
                        getGrpcMethod(),
                        asyncUnaryCall(
                                new MethodHandlers<
                                        ReqT,
                                        RespT
                                        >(
                                        serviceInstance, methodName)))
                .build();
    }

    private ServiceDescriptor getServiceDescriptor() {
        ServiceDescriptor result;
        if ((result = getGrpcServiceDescriptorInstance) == null) {

            synchronized (BrpcGrpcServiceBinder.class) {
                if ((result = getGrpcServiceDescriptorInstance) == null) {

                    result = getGrpcServiceDescriptorInstance = io.grpc.ServiceDescriptor.newBuilder(fullServiceName)
                            .setSchemaDescriptor(new GrpcServiceFileDescriptorSupplier(protoFileDescriptor))
                            .addMethod(getGrpcMethod())
                            .build();
                }
            }
        }
        return result;
    }

    final class MethodHandlers<ReqRawT, RespRawT> implements
            ServerCalls.UnaryMethod<ReqRawT, RespRawT>,
            ServerCalls.ServerStreamingMethod<ReqRawT, RespRawT>,
            ServerCalls.ClientStreamingMethod<ReqRawT, RespRawT>,
            ServerCalls.BidiStreamingMethod<ReqRawT, RespRawT> {

        private Object serviceInstance;

        private String methodName;

        public MethodHandlers(Object serviceInstance, String methodName) {
            this.serviceInstance = serviceInstance;
            this.methodName = methodName;
        }

        @Override
        public StreamObserver<ReqRawT> invoke(StreamObserver<RespRawT> streamObserver) {

            throw new AssertionError();
        }

        @Override
        public void invoke(ReqRawT reqRawT, StreamObserver<RespRawT> streamObserver) {
            try {

                Method method = serviceClazz.getMethod(methodName, requestDefaultClazz);
                RespRawT response = (RespRawT) method.invoke(serviceInstance, (ReqT) reqRawT);
                streamObserver.onNext(response);
                streamObserver.onCompleted();

            } catch (Exception e) {
                e.printStackTrace();
                streamObserver.onError(e);
            }
        }

    }

}


abstract class GrpcServiceBaseDescriptorSupplier
        implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GrpcServiceBaseDescriptorSupplier() {
    }

    public GrpcServiceBaseDescriptorSupplier(Descriptors.FileDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    private Descriptors.FileDescriptor descriptor;

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
        return descriptor;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
        return getFileDescriptor().findServiceByName("EchoService");
    }
}

final class GrpcServiceFileDescriptorSupplier
        extends GrpcServiceBaseDescriptorSupplier {
    public GrpcServiceFileDescriptorSupplier(Descriptors.FileDescriptor descriptor) {
        super(descriptor);
    }
}

final class GrpcServiceMethodDescriptorSupplier
        extends GrpcServiceBaseDescriptorSupplier
        implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {

    private final String methodName;

    GrpcServiceMethodDescriptorSupplier(String methodName) {
        this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
        return getServiceDescriptor().findMethodByName(methodName);
    }
}


