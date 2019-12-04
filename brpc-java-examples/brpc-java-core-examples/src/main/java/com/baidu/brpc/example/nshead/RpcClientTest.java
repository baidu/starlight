package com.baidu.brpc.example.nshead;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.standard.Echo;
import com.baidu.brpc.example.standard.Echo.EchoResponse;
import com.baidu.brpc.example.standard.EchoService;
import com.baidu.brpc.protocol.Options.ProtocolType;

public class RpcClientTest {

    public static void main(String[] args) {

        RpcClientOptions clientOption = RpcClientOptions.builder()
              .protocolType(ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE)
              //.protocolType(ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE)
              .writeTimeoutMillis(1000)
              .readTimeoutMillis(5000)
              .loadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR)
              .encoding("gbk")
              .build();

        // 高端口，在开发机上测试
        String serviceUrl = "list://localhost:8080";

        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption);

        // sync call
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);

        RpcContext.getContext().setLogId(1234);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello world").build();
        EchoResponse response = echoService.echo(request);
        System.out.println("--------nshead protobuf sync call response-----------------");
        System.out.println(response.getMessage());
        rpcClient.stop();


    }
}
