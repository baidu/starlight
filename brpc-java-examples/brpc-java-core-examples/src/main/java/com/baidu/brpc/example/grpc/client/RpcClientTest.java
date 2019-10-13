package com.baidu.brpc.example.grpc.client;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.http.json.EchoService;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;

import java.util.ArrayList;
import java.util.List;

public class RpcClientTest {
    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_GRPC_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(5000);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setMaxTryTimes(1);

        String serviceUrl = "list://127.0.0.1:50051";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();;
        interceptors.add(new CustomInterceptor());

        // sync call
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        try {
            String response = echoService.hello("okok");
            System.out.printf("sync call success, response=%s\n", response);
        } catch (RpcException ex) {
            System.out.println("sync call failed, msg=" + ex.getMessage());
        }
        rpcClient.stop();
    }
}
