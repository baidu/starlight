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

import com.baidu.cloud.starlight.api.extension.impl.Ext1Impl1;
import com.baidu.cloud.starlight.api.extension.impl.Ext1Impl2;
import com.baidu.cloud.starlight.api.extension.impl.Ext1Impl3;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class ExtensionLoaderTest {

    @Test
    public void test_getExtension() throws Exception {
        assertTrue(ExtensionLoader.getInstance(Ext1.class).getExtension("impl1") instanceof Ext1Impl1);
        assertTrue(ExtensionLoader.getInstance(Ext1.class).getExtension("impl2") instanceof Ext1Impl2);
        assertTrue(ExtensionLoader.getInstance(Ext1.class).getExtension("xxx") instanceof Ext1Impl3);
    }

    @Test
    public void test_getExtension_Run() throws Exception {
        String[] impls = new String[] {"impl1", "impl2", "xxx"};
        int i = 1;
        for (String impl : impls) {
            Ext1 x = ExtensionLoader.getInstance(Ext1.class).getExtension(impl);
            assertEquals("bang" + i++, x.bang(null, 1));
            assertTrue(ExtensionLoader.getInstance(Ext1.class).hasExtension(impl));
        }

        assertEquals("impl1", ExtensionLoader.getInstance(Ext1.class).getExtensionName(new Ext1Impl1()));
        assertArrayEquals(impls, ExtensionLoader.getInstance(Ext1.class).getLoadedExtensions().toArray(new String[0]));
        assertArrayEquals(impls,
            ExtensionLoader.getInstance(Ext1.class).getSupportedExtensions().toArray(new String[0]));
        assertEquals(ExtensionLoader.class.getName() + "[" + Ext1.class.getName() + "]",
            ExtensionLoader.getInstance(Ext1.class).toString());

    }

}