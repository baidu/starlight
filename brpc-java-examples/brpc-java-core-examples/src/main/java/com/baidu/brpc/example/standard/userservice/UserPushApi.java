package com.baidu.brpc.example.standard.userservice;

public interface UserPushApi {

    PushResult clientReceive(PushData data, String clientName);
}
