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
 
package com.baidu.cloud.starlight.api.extension;

/**
 * ClassLoadContext: Migrate from Stargate {@link IClassLoadStrategy#getClassLoader(ClassLoadContext)}.
 */
@SuppressWarnings("rawtypes")
public class ClassLoadContext {
    private final Class m_caller;

    /**
     * Returns the class representing the caller of {@link ClassLoaderResolver} API. Can be used to retrieve the
     * caller's classloader etc (which may be different from the ClassLoaderResolver's own classloader).
     */
    public final Class getCallerClass() {
        return m_caller;
    }

    /**
     * This constructor is package-private to restrict instantiation to {@link ClassLoaderResolver} only.
     */
    ClassLoadContext(final Class caller) {
        m_caller = caller;
    }

}