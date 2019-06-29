package com.baidu.brpc.example.push.push;

public interface UserPushApi {

    PushResult clientReceive(String extra, PushData data);

}
