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
 
package com.baidu.cloud.starlight.api.exception;

/**
 * Transport Exception, Distinguish from Biz Exception {@link StarlightRpcException} Just Throw Used in
 * ClientPeer\ServerPeer\RpcHandler Created by liuruisen on 2020/2/5.
 */
public class TransportException extends RpcException {

    public static final Integer CONNECT_EXCEPTION = 2001;

    public static final Integer WRITE_EXCEPTION = 2002;

    public static final Integer BODY_NULL_EXCEPTION = 2003;

    public static final Integer HEARTBEAT_EXCEPTION = 2004;

    public static final Integer RPC_CHANNEL_NULL_EXCEPTION = 2005;

    public static final Integer BIND_EXCEPTION = 2006;

    public static final Integer SHUTTING_DOWN = 2007;

    public TransportException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public TransportException(Integer code, String message) {
        super(code, message);
    }

    public TransportException(String message) {
        super(message);
    }

}
