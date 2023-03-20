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

import feign.MethodMetadata;
import feign.RequestTemplate;
import feign.Target;
import feign.querymap.FieldQueryMapEncoder;
import feign.spring.SpringContract;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by liuruisen on 2020/7/15.
 */
public class RequestTemplateArgsResolverTest {

    @Test
    public void create() {

        List<MethodMetadata> metadats = new SpringContract().parseAndValidateMetadata(SpringRestService.class);
        RequestTemplateArgsResolver resolver = new RequestTemplateArgsResolver(new FieldQueryMapEncoder(),
            Target.EmptyTarget.create(SpringRestService.class));

        for (MethodMetadata metadata : metadats) {
            switch (metadata.method().getName().toLowerCase()) {
                case "get":
                    RequestTemplate template = resolver.create(new Object[] {"Test", "Test"}, metadata);
                    Assert.assertTrue(template.queries().size() == 1);
                    Assert.assertTrue(template.queries().get("query").contains("Test"));
                    break;
                case "delete":
                    RequestTemplate template1 = resolver.create(new Object[] {"Test"}, metadata);
                    Assert.assertTrue(template1.path().contains("Test"));
                    break;
                case "getquerymap":
                    RequestTemplate template2 = resolver.create(new Object[] {"Test1", "Test2"}, metadata);
                    Assert.assertTrue(template2.queries().size() == 2);
                    Assert.assertTrue(template2.queries().get("query1").contains("Test1"));
                    Assert.assertTrue(template2.queries().get("query2").contains("Test2"));
                    break;
            }
        }

    }

}