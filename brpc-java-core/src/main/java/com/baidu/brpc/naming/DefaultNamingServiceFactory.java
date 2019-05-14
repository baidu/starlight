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
package com.baidu.brpc.naming;

public class DefaultNamingServiceFactory implements NamingServiceFactory {
    public NamingService createNamingService(BrpcURL url) {
        String schema = url.getSchema();
        if ("list".equals(schema)) {
            return new ListNamingService(url);
        } else if ("file".equals(schema)) {
            return new FileNamingService(url);
        } else if ("dns".equals(schema)) {
            return new DnsNamingService(url);
        } else {
            throw new IllegalArgumentException("schema is not valid:" + schema);
        }
    }
}
