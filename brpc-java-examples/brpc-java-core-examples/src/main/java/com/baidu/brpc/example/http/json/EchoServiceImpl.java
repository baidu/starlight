package com.baidu.brpc.example.http.json;

public class EchoServiceImpl implements EchoService {

    @Override
    public String hello(String request) {
        //        throw new RuntimeException("failed");
        return "hello " + request;
    }

    @Override
    public String hello2(String userName, Integer userId) {
        return "userName=" + userName + ", userId=" + userId;
    }

    @Override
    public Echo hello3(Echo echo) {
        return new Echo("hello " + echo.getMessage(), echo.getTime());
    }
}
