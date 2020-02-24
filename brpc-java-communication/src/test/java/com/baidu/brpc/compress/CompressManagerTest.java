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

package com.baidu.brpc.compress;

import com.baidu.brpc.protocol.Options;
import org.junit.Assert;
import org.junit.Test;

public class CompressManagerTest {
    @Test
    public void testGetCompress() {
        CompressManager compressManager = CompressManager.getInstance();
        Compress compress = compressManager.getCompress(Options.CompressType.COMPRESS_TYPE_NONE_VALUE);
        Assert.assertNotNull(compress);
        compress = compressManager.getCompress(Options.CompressType.COMPRESS_TYPE_GZIP_VALUE);
        Assert.assertNotNull(compress);
    }
}
