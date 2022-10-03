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
 * Created by liuruisen on 2020/2/5.
 */
public class CodecException extends RpcException {

    public static final Integer PROTOCOL_ENCODE_EXCEPTION = 3001;

    public static final Integer PROTOCOL_DECODE_EXCEPTION = 3002;

    public static final Integer SERIALIZE_EXCEPTION = 3003;

    public static final Integer DESERIALIZE_EXCEPTION = 3004;

    public static final Integer PROTOCOL_DECODE_NOTMATCH_EXCEPTION = 3005;

    public static final Integer PROTOCOL_DECODE_NOTENOUGHDATA_EXCEPTION = 3006;

    public static final Integer PROTOCOL_INSUFFICIENT_DATA_EXCEPTION = 3011;

    public static final Integer COMPRESS_EXCEPTION = 3007;

    public static final Integer DECOMPRESS_EXCEPTION = 3008;

    public static final Integer BODY_ENCODE_EXCEPTION = 3009;

    public static final Integer BODY_DECODE_EXCEPTION = 3010;

    public CodecException(String message) {
        super(message);
    }

    public CodecException(Integer code, String message) {
        super(code, message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodecException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
