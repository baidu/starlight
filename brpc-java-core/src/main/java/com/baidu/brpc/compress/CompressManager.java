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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompressManager {
    private static final int MAX_COMPRESS_NUM = 16;
    private static volatile CompressManager instance;
    private Compress[] compressArray;
    private int compressNum;

    public static CompressManager getInstance() {
        if (instance == null) {
            synchronized (CompressManager.class) {
                if (instance == null) {
                    instance = new CompressManager();
                }
            }
        }
        return instance;
    }

    private CompressManager() {
        compressArray = new Compress[MAX_COMPRESS_NUM];
        compressArray[Options.CompressType.COMPRESS_TYPE_NONE_VALUE] = new NoneCompress();
        compressArray[Options.CompressType.COMPRESS_TYPE_GZIP_VALUE] = new GzipCompress();
        compressArray[Options.CompressType.COMPRESS_TYPE_ZLIB_VALUE] = new ZlibCompress();
        compressArray[Options.CompressType.COMPRESS_TYPE_SNAPPY_VALUE] = new SnappyCompress();
        compressNum = 4;
    }

    public Compress getCompress(int compressType) {
        if (compressType < 0 || compressType >= compressNum) {
            throw new RuntimeException("out of bound");
        }
        Compress compress = compressArray[compressType];
        if (compress == null) {
            String errMsg = String.format("compress type=%d not support", compressType);
            log.warn(errMsg);
            throw new RuntimeException(errMsg);
        }
        return compress;
    }
}
