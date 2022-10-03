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

import com.baidu.cloud.thirdparty.feign.MethodMetadata;
import com.baidu.cloud.thirdparty.feign.RequestTemplate;
import com.baidu.cloud.thirdparty.feign.Target;
import com.baidu.cloud.thirdparty.feign.querymap.FieldQueryMapEncoder;
import com.baidu.cloud.thirdparty.feign.spring.SpringContract;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.protocol.http.User;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.baidu.cloud.starlight.api.exception.CodecException.PROTOCOL_ENCODE_EXCEPTION;

/**
 * Created by liuruisen on 2020/7/15.
 */
public class EncodedRequestTemplateArgsResolverTest {

    @Test
    public void resolve() {
        List<MethodMetadata> metadats = new SpringContract().parseAndValidateMetadata(SpringRestService.class);
        EncodedRequestTemplateArgsResolver resolver = new EncodedRequestTemplateArgsResolver(new FieldQueryMapEncoder(),
            Target.EmptyTarget.create(SpringRestService.class), new JsonSerializer());

        for (MethodMetadata metadata : metadats) {
            if (metadata.method().getName().equalsIgnoreCase("post")) {
                User user = new User();
                user.setName("Test");
                RequestTemplate template = resolver.create(new Object[] {user}, metadata);
                Assert.assertTrue(template.body().length > 0);
                break;
            }
        }
    }

    @Test
    public void resolveError() {
        List<MethodMetadata> metadats = new SpringContract().parseAndValidateMetadata(SpringRestService.class);
        EncodedRequestTemplateArgsResolver resolver = new EncodedRequestTemplateArgsResolver(new FieldQueryMapEncoder(),
            Target.EmptyTarget.create(SpringRestService.class), new JsonSerializer());

        for (MethodMetadata metadata : metadats) {
            if (metadata.method().getName().equalsIgnoreCase("post")) {
                try {
                    RequestTemplate template = resolver.create(new Object[] {"Test"}, metadata);
                } catch (Exception e) {
                    Assert.assertTrue(e instanceof CodecException);
                    Assert.assertTrue(((CodecException) e).getCode() == PROTOCOL_ENCODE_EXCEPTION);
                }
                break;
            }
        }
    }
}