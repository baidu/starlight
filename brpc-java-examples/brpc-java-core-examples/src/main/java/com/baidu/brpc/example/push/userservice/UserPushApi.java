package com.baidu.brpc.example.push.userservice;

public interface UserPushApi {

    PushResult clientReceive(String extra, PushData data);

}
