package com.baidu.brpc.example.grpc.grpcserver;

import com.baidu.brpc.example.standard.Echo;
import com.baidu.brpc.example.standard.EchoServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcTestServer {

    private final int port;
    private final Server server;

    public GrpcTestServer(int port) {
        this(ServerBuilder.forPort(port),port);
    }

    public GrpcTestServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        server = serverBuilder.addService(new EchoService())
                .build();
    }

    public void start() throws IOException {
        server.start();
        log.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    GrpcTestServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }



    public static void main(String[] args) throws IOException, InterruptedException {
        GrpcTestServer server = new GrpcTestServer(50051);
        server.start();
        server.blockUntilShutdown();
    }

    private static class EchoService extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public void echo(Echo.EchoRequest request, StreamObserver<Echo.EchoResponse> responseObserver) {
            String message = request.getMessage();
            Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                    .setMessage("Hello00o: "+message)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
