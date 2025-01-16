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

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.thirdparty.feign.MethodMetadata;
import com.baidu.cloud.thirdparty.feign.QueryMapEncoder;
import com.baidu.cloud.thirdparty.feign.RequestTemplate;
import com.baidu.cloud.thirdparty.feign.Target;
import com.baidu.cloud.thirdparty.feign.Util;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import java.util.Map;

/**
 * form表单编码器
 */
public class FormRequestTemplateArgsResolver extends RequestTemplateArgsResolver {

    private Serializer encoder = new JsonSerializer();

    public FormRequestTemplateArgsResolver(QueryMapEncoder queryMapEncoder, Target target) {
        super(queryMapEncoder, target);
    }

    @Override
    protected RequestTemplate resolve(Object[] argv, RequestTemplate mutable, Map<String, Object> variables,
        MethodMetadata metadata) {

        Object body = argv[metadata.bodyIndex()];
        Util.checkArgument(body != null, "Body parameter %s was null", metadata.bodyIndex());
        try {
            mutable.body(encoder.serialize(body, metadata.bodyType()), Util.UTF_8); // serialize and set request body
        } catch (Exception e) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
                "Encode request with SpringRestProtocol failed, BuildEncodedTemplateFromArgs failed: "
                    + e.getMessage());
        }
        return super.resolve(argv, mutable, variables, metadata);
    }
}
