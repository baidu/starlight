package com.baidu.brpc.push.userservice;

public interface UserPushApi {

    PushResult clientReceive(PushData data);
}
