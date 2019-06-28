package com.baidu.brpc.example.push.userservice;


public class UserPushApiImplII implements UserPushApi {

    @Override
    public PushResult clientReceive(String extra, PushData data) {
        // System.out.println("++invoke clientReceive :" + data.getData());
        PushResult pushResult = new PushResult();
        pushResult.setResult(extra + " UserPushApiImplII.clientReceive got data:" + data.getData());
        return pushResult;
    }

}
