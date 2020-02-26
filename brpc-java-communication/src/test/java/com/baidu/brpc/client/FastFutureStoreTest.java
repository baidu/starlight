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

package com.baidu.brpc.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wanghongfei on 2018/12/10.
 */
public class FastFutureStoreTest {
    private FastFutureStore store = new FastFutureStore(10);

    @Test
    public void testPutAndRemove() {
        // 100 is much bigger than cap(10)
        round(100);
    }

    @Test
    public void testMultipleRemoval() {
        RpcFuture fut = new RpcFuture();
        // fut will be mapped to slot 0
        long logId = store.put(fut);
        Assert.assertSame(fut, store.get(0));

        // logId 10 will be mapped to slot 0 as well
        RpcFuture removedFut = store.getAndRemove(10);
        // cannot remove RpcFuture with logId 10
        Assert.assertNull(removedFut);

        // fut can only be removed with the correct logId
        removedFut = store.getAndRemove(logId);
        Assert.assertSame(removedFut, fut);

        removedFut = store.getAndRemove(logId);
        Assert.assertNull(removedFut);
    }

    private void round(int times) {
        List<RpcFuture> elemList = new ArrayList<RpcFuture>(10);
        List<Long> idList = new ArrayList<Long>();

        for (int ix = 0; ix < times; ++ix) {
            RpcFuture fut = new RpcFuture();
            Long id = store.put(fut);

            elemList.add(fut);
            idList.add(id);
        }

        for (int ix = 0; ix < idList.size(); ++ix) {
            Long id = idList.get(ix);
            RpcFuture fut = store.getAndRemove(id);
            RpcFuture expected = elemList.get(ix);

            Assert.assertSame(fut, expected);
            Assert.assertNull(store.get(id));
        }

    }

}
