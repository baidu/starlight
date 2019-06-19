package com.baidu.brpc.example.push.userservice;

public interface UserPushApi {

    PushResult clientReceive(PushData data);
}
