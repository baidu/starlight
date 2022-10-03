package com.baidu.cloud.starlight.benchmark.serializer.protobuf.media;

/**
 * Created by liuruisen on 2019/11/5.
 */
public abstract class MediaMessage<T> {

    private T mediaContent;

    private byte[] messageBytes;

    public MediaMessage() {
        this.setMediaContent(createMediaContent());
    }

    protected abstract T createMediaContent();

    public T getMediaContent() {
        return mediaContent;
    }

    public void setMediaContent(T mediaContent) {
        this.mediaContent = mediaContent;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }

    public void setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
    }
}
