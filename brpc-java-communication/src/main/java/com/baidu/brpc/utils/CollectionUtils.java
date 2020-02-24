/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.utils;

import java.util.Collection;

/**
 * Utility methods for {@link Collection} instances.
 *
 * @author guohaoice@gmail.com
 */
public final class CollectionUtils {

    /**
     * Null-safe check if the specified collection is empty.
     *
     * @param collection the collection to check, may be null
     * @return true if empty or null
     */
    public static boolean isEmpty(Collection<?> collection) {
        return null == collection || collection.isEmpty();
    }

    /**
     * Null-safe check if the specified collection is not empty.
     *
     * @param collection the collection to check, may be null
     * @return true if non-null and non-empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
}
