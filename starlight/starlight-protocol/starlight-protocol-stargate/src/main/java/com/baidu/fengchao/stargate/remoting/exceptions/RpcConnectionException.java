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
 
package com.baidu.fengchao.stargate.remoting.exceptions;

public class RpcConnectionException extends RpcSystemException {
    private static final long serialVersionUID = 1927415247924574028L;

    public RpcConnectionException(String msg) {
        super(msg);
    }

    public RpcConnectionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
