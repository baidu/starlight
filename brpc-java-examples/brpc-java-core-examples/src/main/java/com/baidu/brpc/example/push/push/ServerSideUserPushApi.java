package com.baidu.brpc.example.push.push;

public interface ServerSideUserPushApi extends UserPushApi {

    /**
     * server端接口， 多一个clientName参数
     *
     * @param clientName
     * @param data
     *
     * @return
     */
    PushResult clientReceive(String clientName, String extra, PushData data);

}
