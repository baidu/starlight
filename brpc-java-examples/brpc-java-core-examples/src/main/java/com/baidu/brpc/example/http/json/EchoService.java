package com.baidu.brpc.example.http.json;

import com.baidu.brpc.protocol.BrpcMeta;

public interface EchoService {
    @BrpcMeta(serviceName = "test/hei", methodName = "abc")
    String hello(String request);
    String hello2(String userName, Integer userId);
}
