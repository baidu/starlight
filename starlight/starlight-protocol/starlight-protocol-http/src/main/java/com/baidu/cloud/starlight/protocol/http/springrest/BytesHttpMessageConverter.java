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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.protocol.http.HttpDecoder;
import com.baidu.cloud.thirdparty.springframework.http.MediaType;
import com.baidu.cloud.thirdparty.springframework.http.converter.ByteArrayHttpMessageConverter;

import java.util.Collections;

/**
 * Subclass of spring-mvc {@link com.baidu.cloud.thirdparty.springframework.http.converter.HttpMessageConverter}. Read
 * body bytes from HttpMessage, but not change data format(not decode).
 * 
 * @see ByteArrayHttpMessageConverter#readInternal The decode operation takes place in
 *      {@link HttpDecoder#decodeBody(MsgBase)} Created by liuruisen on 2020/6/8.
 */
public class BytesHttpMessageConverter extends ByteArrayHttpMessageConverter {

    public BytesHttpMessageConverter() {
        setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }
}
