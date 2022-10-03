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
 
package com.baidu.cloud.starlight.api.extension.impl;

import com.baidu.cloud.starlight.api.extension.Ext1;
import com.baidu.cloud.starlight.api.common.URI;

public class Ext1Impl3 implements Ext1 {
    public String echo(URI uri, String s) {
        return "echo3";
    }

    public String yell(URI uri, String s) {
        return "yell3";
    }

    public String bang(URI uri, int i) {
        return "bang3";
    }
}
