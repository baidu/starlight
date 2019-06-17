package com.baidu.brpc.protocol.push;

public abstract class ServerPushPacket {

    protected SPHead spHead; // spHead请求头

    public SPHead getSpHead() {
        return spHead;
    }

    public void setSpHead(SPHead spHead) {
        this.spHead = spHead;
    }

}
