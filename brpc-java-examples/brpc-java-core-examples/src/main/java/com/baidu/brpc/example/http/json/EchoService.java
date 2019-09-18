package com.baidu.brpc.example.http.json;

public interface EchoService {
    String hello(String request);
    String hello2(String userName, Integer userId);
}
