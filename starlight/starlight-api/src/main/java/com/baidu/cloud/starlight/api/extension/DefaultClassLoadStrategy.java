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
 * Migrate from Stargate DefaultClassLoadStrategy 默认选择策略,基本可以满足大部分的情况
 */
public class DefaultClassLoadStrategy implements IClassLoadStrategy {

    public ClassLoader getClassLoader(final ClassLoadContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("null input: ctx");
        }
        final ClassLoader callerLoader = ctx.getCallerClass().getClassLoader();
        final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

        ClassLoader result;

        // if 'callerLoader' and 'contextLoader' are in a parent-child
        // relationship, always choose the child:

        if (isChild(contextLoader, callerLoader)) {
            result = callerLoader;
        } else if (isChild(callerLoader, contextLoader)) {
            result = contextLoader;
        } else {
            // this else branch could be merged into the previous one,
            // but I show it here to emphasize the ambiguous case:
            result = contextLoader;
        }

        final ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

        // precaution for when deployed as a bootstrap or extension class:
        if (isChild(result, systemLoader)) {
            result = systemLoader;
        }
        return result;
    }

    /**
     * Returns 'true' if 'loader2' is a delegation child of 'loader1' [or if 'loader1'=='loader2']. Of course, this
     * works only for classloaders that set their parent pointers correctly. 'null' is interpreted as the primordial
     * loader [i.e., everybody's parent].
     */
    private static boolean isChild(final ClassLoader loader1, ClassLoader loader2) {
        if (loader1 == loader2) {
            return true;
        }
        if (loader2 == null) {
            return false;
        }
        if (loader1 == null) {
            return true;
        }

        for (; loader2 != null; loader2 = loader2.getParent()) {
            if (loader2 == loader1) {
                return true;
            }
        }
        return false;
    }

}