package com.baidu.brpc.example.http.json;

public class EchoServiceImpl implements EchoService {

    @Override
    public String hello(String request) {
        return "hello " + request;
    }
}
