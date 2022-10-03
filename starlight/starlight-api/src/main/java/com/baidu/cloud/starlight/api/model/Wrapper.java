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
 
package com.baidu.cloud.starlight.api.model;

/**
 * Used to wrapper multi params. Used when want to support brpc multi-parameter calls and generic calls NOTICE: When
 * used in brpc multi-parameter, both of the server and client must be Starlight Created by liuruisen on 2020/2/14.
 */
public class Wrapper<T> {

    private T obj;

    public Wrapper(T obj) {
        this.obj = obj;
    }

    public T getObj() {
        return obj;
    }
}
