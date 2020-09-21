package com.baidu.brpc.example.http.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options.ProtocolType;

public class RpcClientTest {
    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(ProtocolType.PROTOCOL_HTTP_JSON_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(30000);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setMaxTryTimes(1);

        String serviceUrl = "list://127.0.0.1:8080";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new CustomInterceptor());

        // sync call
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        try {
            String response = echoService.hello("okok");
            System.out.printf("sync call hello success, response=%s\n", response);

            response = echoService.hello2("jack", 123);
            System.out.printf("sync call hello2 success, response=%s\n", response);

            final Echo echoResp = echoService.hello3(new Echo("foo", new Date()));
            System.out.printf("sync call hello3 success, response=%s\n", echoResp);
        } catch (RpcException ex) {
            System.out.println("sync call failed, msg=" + ex.getMessage());
        }
        rpcClient.stop();

        // async call
        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        RpcCallback callback = new RpcCallback<String>() {
            @Override
            public void success(String response) {
                if (response != null) {
                    System.out.printf("async call success, response=%s\n", response);
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
        try {
            Future<String> future = echoServiceAsync.hello("ok", callback);
            Future<String> future2 = echoServiceAsync.hello2("peter", 234, callback);
            try {
                if (future != null) {
                    future.get();
                }
                if (future2 != null) {
                    future2.get();
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
