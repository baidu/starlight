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
 
package com.baidu.cloud.starlight.serialization.utils;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.utils.StringUtils;

public class SerializationUtils {

    public static CodecException convertToCodecException(Throwable throwable, Integer errorCode, String errMsgPrefix) {
        StringBuilder errMsgSb = new StringBuilder(errMsgPrefix);

        // parent error detail
        errMsgSb.append(", error detail: ");
        errMsgSb.append(throwable.getClass().getName());
        if (!StringUtils.isEmpty(throwable.getMessage())) {
            errMsgSb.append(", ");
            errMsgSb.append(throwable.getMessage());
        }

        errMsgSb.append(" ");

        // cause error detail
        Throwable cause = throwable.getCause();
        if (cause != null) {
            errMsgSb.append("caused by: ");
            errMsgSb.append(cause.getClass().getName());
            errMsgSb.append(", ");
            errMsgSb.append(cause.getMessage());
        }

        return new CodecException(errorCode, errMsgSb.toString(), throwable);
    }
}