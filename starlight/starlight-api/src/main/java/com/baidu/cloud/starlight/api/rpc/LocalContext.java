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
 
package com.baidu.cloud.starlight.api.rpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used to store app ClassLoader for hairuo, currently. Created by liuruisen on 2020/9/15.
 */
public class LocalContext {

    private static final ConcurrentMap<String, LocalContext> CONTEXTS = new ConcurrentHashMap<String, LocalContext>();

    public static LocalContext getContext(String id) {
        LocalContext context = CONTEXTS.get(id);
        if (context == null) {
            LocalContext newContext = new LocalContext();
            context = CONTEXTS.putIfAbsent(id, newContext);
            if (context == null) {
                // put succeeded, use new value
                context = newContext;
            }
        }
        return context;
    }

    private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    public void set(String key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

}
