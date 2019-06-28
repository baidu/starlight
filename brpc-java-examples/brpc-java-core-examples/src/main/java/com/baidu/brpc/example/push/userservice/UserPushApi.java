package com.baidu.brpc.example.push.userservice;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.server.RpcServer;

public interface UserPushApi {

    PushResult clientReceive(String extra, PushData data);

}
