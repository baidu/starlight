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
 * This non-instantiable non-subclassable class acts as the global point for choosing a ClassLoader for dynamic
 * class/resource loading at any point in an application. Migrate from Stargate
 * 
 * @see ResourceLoader
 * @see IClassLoadStrategy
 * @see DefaultClassLoadStrategy
 * 
 */
@SuppressWarnings("rawtypes")
public abstract class ClassLoaderResolver {

    /**
     * This method selects the "best" classloader instance to be used for class/resource loading by whoever calls this
     * method. The decision typically involves choosing between the caller's current, thread context, system, and other
     * classloaders in the JVM and is made by the {@link IClassLoadStrategy} instance established by the last call to
     * {@link #setStrategy}.
     *
     * @return classloader to be used by the caller ['null' indicates the primordial loader]
     */
    public static synchronized ClassLoader getClassLoader() {
        final Class caller = getCallerClass(0);
        final ClassLoadContext ctx = new ClassLoadContext(caller);

        return s_strategy.getClassLoader(ctx);
    }

    /**
     * A package-private version of {@link #getClassLoader()} with adjustable caller offset (useful for embedding this
     * method in other library classes in this package).
     *
     * @param callerOffset extra call context depth offset to pass into getCallerClass().
     */
    static synchronized ClassLoader getClassLoader(final int callerOffset) {
        final Class caller = getCallerClass(callerOffset);
        final ClassLoadContext ctx = new ClassLoadContext(caller);

        return s_strategy.getClassLoader(ctx);
    }

    /**
     * Gets the current classloader selection strategy setting.
     */
    public static synchronized IClassLoadStrategy getStrategy() {
        return s_strategy;
    }

    /**
     * Sets the classloader selection strategy to be used by subsequent calls to {@link #getClassLoader()}. An instance
     * of {@link DefaultClassLoadStrategy} is in effect if this method is never called.
     *
     * @param strategy [may not be null]
     * @return previous setting
     */
    public static synchronized IClassLoadStrategy setStrategy(final IClassLoadStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("null input: strategy");
        }
        final IClassLoadStrategy old = s_strategy;
        s_strategy = strategy;

        return old;
    }

    /**
     * A helper class to get the call context. It subclasses SecurityManager to make getClassContext() accessible. An
     * instance of CallerResolver only needs to be created, not installed as an actual security manager.
     */
    private static final class CallerResolver extends SecurityManager {
        protected Class[] getClassContext() {
            return super.getClassContext();
        }

    }

    private ClassLoaderResolver() {}

    /*
     * Indexes into the current method call context with a given offset.
     */

    private static Class getCallerClass(final int callerOffset) {
        return CALLER_RESOLVER.getClassContext()[CALL_CONTEXT_OFFSET + callerOffset];
    }

    private static IClassLoadStrategy s_strategy; // initialized in <clinit>

    private static final int CALL_CONTEXT_OFFSET = 3; // may need to change if
                                                      // this class is
                                                      // redesigned

    private static final CallerResolver CALLER_RESOLVER; // set in <clinit>

    static {
        try {
            // this can fail if the current SecurityManager does not allow
            // RuntimePermission ("createSecurityManager"):
            CALLER_RESOLVER = new CallerResolver();
        } catch (SecurityException se) {
            throw new RuntimeException("ClassLoaderResolver: could not create CallerResolver: " + se);
        }

        s_strategy = new DefaultClassLoadStrategy();
    }

}
