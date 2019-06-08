package com.baidu.brpc.example.grpc;

import com.baidu.brpc.example.standard.Echo;
import com.baidu.brpc.example.standard.EchoServiceGrpc;
import io.grpc.stub.StreamObserver;

public class EchoService extends EchoServiceGrpc.EchoServiceImplBase {
    @Override
    public void echo(Echo.EchoRequest request, StreamObserver<Echo.EchoResponse> responseObserver) {
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("Hello "+ request.getMessage()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
