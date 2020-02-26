package com.baidu.brpc.push.userservice;

public interface ServerSideUserPushApi extends UserPushApi {

    /**
     * server端接口， 多一个clientName参数
     *
     * @param clientName
     * @param data
     *
     * @return
     */
    PushResult clientReceive(String clientName, PushData data);

}
