package com.baidu.brpc.example.http.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.example.standard.Echo;
import com.baidu.brpc.example.standard.Echo.EchoRequest;
import com.baidu.brpc.example.standard.Echo.EchoResponse;
import com.baidu.brpc.example.standard.EchoService;
import com.baidu.brpc.example.standard.EchoServiceAsync;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options.ProtocolType;

public class RpcClientTest {

    public static void main(String[] args) {
        RpcClientOptions clientOption = RpcClientOptions.builder()
              .protocolType(ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE)
              .writeTimeoutMillis(1000)
              .readTimeoutMillis(500)
              .loadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR)
              .maxTryTimes(1)
              .build();

        String serviceUrl = "list://127.0.0.1:8080";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();;
        interceptors.add(new CustomInterceptor());

        // sync call
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        String message =
                "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello";
        EchoRequest request = Echo.EchoRequest.newBuilder().setMessage(message)
                .build();
        try {
            EchoResponse response = echoService.echo(request);
            System.out.printf("sync call success, response=%s\n", response.getMessage());
        } catch (RpcException ex) {
            System.out.println("sync call failed, msg=" + ex.getMessage());
        }
        rpcClient.stop();

        // async call
        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        RpcCallback callback = new RpcCallback<EchoResponse>() {
            @Override
            public void success(EchoResponse response) {
                if (response != null) {
                    System.out.printf("async call success, response=%s\n", response.getMessage());
                } else {
                    System.out.println("async call failed");
                }
            }

            @Override
            public void fail(Throwable e) {
                System.out.printf("async call failed, %s\n", e.getMessage());
            }
        };
        EchoServiceAsync echoServiceAsync = BrpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
        request = Echo.EchoRequest.newBuilder().setMessage("hello world async").build();
        try {
            Future<EchoResponse> future = echoServiceAsync.echo(request, callback);
            try {
                if (future != null) {
                    future.get();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (RpcException ex) {
            System.out.println("send exception, ex=" + ex.getMessage());
        }
        rpcClient.stop();
    }
}
