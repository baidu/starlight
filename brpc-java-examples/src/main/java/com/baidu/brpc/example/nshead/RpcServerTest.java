package com.baidu.brpc.example.nshead;

import com.baidu.brpc.example.standard.EchoServiceImpl;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;

public class RpcServerTest {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }

        RpcServerOptions options = new RpcServerOptions();
        options.setProtocolType(Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE);
        // options.setProtocolType(Options.ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE);
        options.setEncoding("gbk");
        RpcServer rpcServer = new RpcServer(port, options);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        // make server keep running
        synchronized (RpcServerTest.class) {
            try {
                RpcServerTest.class.wait();
            } catch (Throwable e) {
                // ignore
            }
        }
    }
}
