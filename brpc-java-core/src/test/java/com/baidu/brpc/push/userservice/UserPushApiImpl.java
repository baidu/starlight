package com.baidu.brpc.push.userservice;

public class UserPushApiImpl implements UserPushApi {

    @Override
    public PushResult clientReceive(PushData data, String clientName) {
        System.out.println("++invoke clientReceive :" + data.getData());
        PushResult pushResult = new PushResult();
        pushResult.setResult("got data:" + data.getData());
        return pushResult;
    }
}
