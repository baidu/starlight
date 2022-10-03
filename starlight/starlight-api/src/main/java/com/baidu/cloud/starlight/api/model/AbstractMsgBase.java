/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.api.model;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by liuruisen on 2020/2/7.
 */
public abstract class AbstractMsgBase implements MsgBase {

    /**
     * Reserve the first 10 bits for special response, such as SHUTTING_DOWN_EVENT
     */
    private static final AtomicLong INVOKE_ID = new AtomicLong(10);

    private long id;

    private Map<String, Object> attachmentKv = new LinkedHashMap<>();

    /**
     * 当前依靠GC机制回收，FIXME 是否进行阅后即焚处理 可存储不随rpc调用传递的信息，如用于记录日志或统计的的数据
     */
    private Map<String, Object> noneAdditionKv = new LinkedHashMap<>();

    private byte[] bodyBytes;

    private Class<?> returnType;

    private boolean heartbeat;

    private int compressType; // default 0: none compress

    private String protocolName;

    private Type genericReturnType;

    public AbstractMsgBase() {
        this.id = INVOKE_ID.getAndIncrement();
    }

    public AbstractMsgBase(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Map<String, Object> getAttachmentKv() {
        return attachmentKv;
    }

    @Override
    public void setAttachmentKv(Map<String, Object> kvMap) {
        this.attachmentKv = kvMap;
    }

    @Override
    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    @Override
    public void setBodyBytes(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    @Override
    public boolean isHeartbeat() {
        return heartbeat;
    }

    @Override
    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    @Override
    public int getCompressType() {
        return compressType;
    }

    @Override
    public void setCompressType(int compressType) {
        this.compressType = compressType;
    }

    @Override
    public String getProtocolName() {
        return protocolName;
    }

    @Override
    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

    @Override
    public Map<String, Object> getNoneAdditionKv() {
        return noneAdditionKv;
    }

    @Override
    public void setNoneAdditionKv(Map<String, Object> kvMap) {
        this.noneAdditionKv = kvMap;
    }

    @Override
    public Type getGenericReturnType() {
        return genericReturnType;
    }

    @Override
    public void setGenericReturnType(Type returnType) {
        this.genericReturnType = returnType;
    }
}
