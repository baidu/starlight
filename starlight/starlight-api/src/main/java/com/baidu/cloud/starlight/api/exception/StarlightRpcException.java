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

import com.baidu.cloud.starlight.api.model.Request;

/**
 * Exceptions during RPC calls will be wrapped as {@link StarlightRpcException} Abstraction of business exception.
 * Created by liuruisen on 2019/12/6.
 */
public class StarlightRpcException extends RpcException {

    public static final Integer MSG_NULL_EXCEPTION = 1000;

    public static final Integer SERVICE_NOT_FOUND_EXCEPTION = 1001;

    public static final Integer METHOD_NOT_FOUND_EXCEPTION = 1002;

    public static final Integer BAD_REQUEST = 4003;

    public static final Integer TIME_OUT_EXCEPTION = 1004;

    public static final Integer INTERNAL_SERVER_ERROR = 1005;

    public static final Integer BIZ_ERROR = 1006;

    public static final Integer UNKNOW = 1007;

    public StarlightRpcException(Integer code, String message) {
        super(code, message);
    }

    public StarlightRpcException(String message) {
        super(message);
    }

    public StarlightRpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public StarlightRpcException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public static StarlightRpcException timeoutException(Request request, String remoteUri) {
        return new StarlightRpcException(StarlightRpcException.TIME_OUT_EXCEPTION,
            "Client call service {" + request.getServiceName() + "} " + "method {" + request.getMethodName()
                + "} time out, remote addr " + remoteUri + ". " + "Maybe caused by: "
                + "1. Biz method execution timeout(4801). " + "2. Abnormal server instance status(4802). "
                + "3. Rpc message delay(4803). " + "4. Unknow reason(4810). ");
    }

}
