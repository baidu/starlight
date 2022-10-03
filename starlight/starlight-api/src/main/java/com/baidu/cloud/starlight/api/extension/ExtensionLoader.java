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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPI ExtensionLoader Created by liuruisen on 2020/2/3.
 */
public class ExtensionLoader<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIR = "META-INF/services/";

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOCATOR = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    private final Class<?> type;

    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    private final Map<String, Class<?>> cachedClasses = new HashMap<>();

    private final ConcurrentMap<String, Object> cachedInstances = new ConcurrentHashMap<>();

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getInstance(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOCATOR.get(type);
        if (loader == null) {
            EXTENSION_LOCATOR.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOCATOR.get(type);
        }
        return loader;
    }

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }

    public String getExtensionName(Class<?> extensionClass) {
        return cachedNames.get(extensionClass);
    }

    @SuppressWarnings("unchecked")
    public synchronized T getExtension(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Object instance = cachedInstances.get(name);
        if (instance == null) {
            instance = createExtension(name);
            cachedInstances.putIfAbsent(name, instance);
        }
        return (T) instance;
    }

    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name == null");
        }
        try {
            return getExtensionClass(name) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<String>(clazzes.keySet()));
    }

    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
    }

    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf =
            new StringBuilder("No such extension " + type.getName() + " by name " + name + ", possible causes: ");
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(entry.getValue());
        }
        return new IllegalStateException(buf.toString());
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " + type
                + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        }
        return clazz;
    }

    private Map<String, Class<?>> getExtensionClasses() {
        if (cachedClasses.size() == 0) {
            synchronized (cachedClasses) {
                if (cachedClasses.size() == 0) {
                    cachedClasses.putAll(loadExtensionClasses());
                }
            }
        }
        return cachedClasses;
    }

    private Map<String, Class<?>> loadExtensionClasses() {
        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        String fileName = SERVICES_DIR + type.getName();
        try {
            Enumeration<URL> urls = ResourceLoader.getResources(fileName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                    try {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            final int ci = line.indexOf('#');
                            if (ci >= 0) {
                                line = line.substring(0, ci);
                            }
                            line = line.trim();
                            if (line.length() > 0) {
                                try {
                                    String name = null;
                                    int i = line.indexOf('=');
                                    if (i > 0) {
                                        name = line.substring(0, i).trim();
                                        line = line.substring(i + 1).trim();
                                    }
                                    Class<?> clazz = ResourceLoader.loadClass(line);
                                    if (!type.isAssignableFrom(clazz)) {
                                        throw new IllegalStateException("Error when load extension class(interface: "
                                            + type + ", class line: " + clazz.getName() + "), class " + clazz.getName()
                                            + "is not subtype of interface.");
                                    }
                                    if (name == null || name.length() == 0) {
                                        if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                            && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                            name = clazz.getSimpleName()
                                                .substring(0,
                                                    clazz.getSimpleName().length() - type.getSimpleName().length())
                                                .toLowerCase();
                                        } else if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                            && clazz.getSimpleName().startsWith(type.getSimpleName())) {
                                            name = clazz.getSimpleName().substring(type.getSimpleName().length())
                                                .toLowerCase();
                                        } else {
                                            throw new IllegalStateException("No such extension name for the class "
                                                + clazz.getName() + " in the config " + url);
                                        }
                                    }
                                    if (!cachedNames.containsKey(clazz)) {
                                        cachedNames.put(clazz, name);
                                    }
                                    Class<?> c = extensionClasses.get(name);
                                    if (c == null) {
                                        extensionClasses.put(name, clazz);
                                    } else if (c != clazz) {
                                        throw new IllegalStateException("Duplicate extension " + type.getName()
                                            + " name " + name + " on " + c.getName() + " and " + clazz.getName());
                                    }
                                } catch (Throwable t) {
                                    IllegalStateException e = new IllegalStateException(
                                        "Failed to load extension class(interface: " + type + ", class line: " + line
                                            + ") in " + url + ", cause: " + t.getMessage(),
                                        t);
                                    exceptions.put(line, e);
                                }
                            }
                        }
                    } finally {
                        reader.close();
                    }
                } catch (Throwable t) {
                    LOG.error("Exception when load extension class(interface: " + type + ", class file: " + url
                        + ") in " + url, t);
                }
            }
        } catch (Throwable t) {
            LOG.error(
                "Exception when load extension class(interface: " + type + ", description file: " + fileName + ").", t);
        }
        return extensionClasses;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}
