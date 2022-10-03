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

import com.baidu.cloud.starlight.protocol.http.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/6/30.
 */
public class SpringRestHandlerMappingTest {

    @Test
    public void getInstance() {
        SpringRestHandlerMapping handlerMapping1 = SpringRestHandlerMapping.getInstance();
        SpringRestHandlerMapping handlerMapping2 = SpringRestHandlerMapping.getInstance();
        Assert.assertTrue(handlerMapping1 == handlerMapping2);
    }

    @Test
    public void createMapping() {
        SpringRestHandlerMapping handlerMapping = SpringRestHandlerMapping.getInstance();
        handlerMapping.createMapping(RestService.class, new RestService() {
            @Override
            public String get(String id, String query) {
                return null;
            }

            @Override
            public String put(User user) {
                return null;
            }

            @Override
            public String delete(String id) {
                return null;
            }

            @Override
            public String post(User user) {
                return null;
            }

            @Override
            public String getQueryMap(String query1, String query2) {
                return null;
            }
        });

        handlerMapping.createMapping(null, null);
    }
}