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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Migrate from Stargate
 */
public class ResourceLoader {

    /**
     * @see ClassLoader#loadClass(String)
     */
    public static Class loadClass(final String name) throws ClassNotFoundException {
        final ClassLoader loader = ClassLoaderResolver.getClassLoader(1);

        return Class.forName(name, false, loader);
    }

    /**
     * @see ClassLoader#getResource(String)
     */
    public static URL getResource(final String name) {
        final ClassLoader loader = ClassLoaderResolver.getClassLoader(1);

        if (loader != null) {
            return loader.getResource(name);
        } else {
            return ClassLoader.getSystemResource(name);
        }
    }

    /**
     * @see ClassLoader#getResourceAsStream(String)
     */
    public static InputStream getResourceAsStream(final String name) {
        final ClassLoader loader = ClassLoaderResolver.getClassLoader(1);

        if (loader != null) {
            return loader.getResourceAsStream(name);
        } else {
            return ClassLoader.getSystemResourceAsStream(name);
        }

    }

    /**
     * @see ClassLoader#getResources(String)
     */
    public static Enumeration<URL> getResources(final String name) throws IOException {
        final ClassLoader loader = ClassLoaderResolver.getClassLoader(1);

        if (loader != null) {
            return loader.getResources(name);
        } else {
            return ClassLoader.getSystemResources(name);
        }
    }

    private ResourceLoader() {}

}