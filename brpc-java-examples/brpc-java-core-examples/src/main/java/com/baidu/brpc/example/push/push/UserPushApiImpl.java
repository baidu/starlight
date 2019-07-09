package com.baidu.brpc.example.push.push;


public class UserPushApiImpl implements UserPushApi {

    @Override
    public PushResult clientReceive(String extra, PushData data) {
        // System.out.println("++invoke clientReceive :" + data.getData());
        PushResult pushResult = new PushResult();
        pushResult.setResult(extra + " receive push data:" + data.getData());
        return pushResult;
    }

}
