package com.baidu.brpc.example.push.userservice;


public class UserPushApiImpl implements UserPushApi {

    @Override
    public PushResult clientReceive(String extra, PushData data) {
        // System.out.println("++invoke clientReceive :" + data.getData());
        PushResult pushResult = new PushResult();
        pushResult.setResult(extra + " UserPushApiImpl.clientReceive got data:" + data.getData());
        return pushResult;
    }

}
