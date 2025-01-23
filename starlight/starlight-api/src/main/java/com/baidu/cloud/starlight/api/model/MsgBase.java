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
import java.util.Map;

/**
 * Created by liuruisen on 2019/12/3.
 */
public interface MsgBase {

    /**
     * Get corelationId: framework id
     * 
     * @return
     */
    long getId();

    /**
     * Set framework id
     * 
     * @param id
     */
    void setId(long id);

    /**
     * kv attachment
     * 
     * @return
     */
    Map<String, Object> getAttachmentKv();

    void setAttachmentKv(Map<String, Object> kvMap);

    /**
     * body data
     * 
     * @return
     */
    byte[] getBodyBytes();

    void setBodyBytes(byte[] bodyByteBuf);

    /**
     * Body data type, used to serializer/deserializer
     * 
     * @return
     */
    Class<?> getReturnType();

    void setReturnType(Class<?> returnType);

    /**
     * Whether it is a heartbeat message
     * 
     * @return
     */
    boolean isHeartbeat();

    void setHeartbeat(boolean heartbeat);

    /**
     * Compress type
     * 
     * @return
     */
    int getCompressType();

    void setCompressType(int compressType);

    /**
     * Get Protocol name, used to decode Get when decoding
     * 
     * @return
     */
    String getProtocolName();

    /**
     * Set Protocol name, used to encode Set when making a request Set when decoding
     * 
     * @param protocolName
     */
    void setProtocolName(String protocolName);

    /**
     * Store kv information for record log, will not send to peer
     * 
     * @return
     */
    Map<String, Object> getNoneAdditionKv();

    /**
     * Store kv information for record log, will not send to peer
     * 
     * @param kvMap
     */
    void setNoneAdditionKv(Map<String, Object> kvMap);

    /**
     * for http decode, TODO 可以精简为只使用这个类型
     * 
     * @return
     */
    Type getGenericReturnType();

    void setGenericReturnType(Type returnType);

}
