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
package com.baidu.brpc.client.pool;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.baidu.brpc.client.channel.BrpcChannel;
import io.netty.channel.Channel;

public class ChannelPoolTest {

    private GenericObjectPool<Channel> pool;

    @Before
    public void before() throws Exception {
        ChannelPooledObjectFactory pooledObjectFactory =
                spy(new ChannelPooledObjectFactory(mock(BrpcChannel.class), "127.0.0.1", 8000));
        doAnswer(new Answer<Channel>() {
            public Channel answer(InvocationOnMock invocation) {
                return mock(Channel.class);
            }
        }).when(pooledObjectFactory).create();
        GenericObjectPoolConfig conf = new GenericObjectPoolConfig();
        conf.setMaxTotal(5);
        conf.setMaxIdle(5);
        conf.setMinIdle(3);
        pool = new GenericObjectPool<Channel>(pooledObjectFactory, conf);
    }

    @After
    public void after() {
        if (pool != null) {
            pool.close();
        }
    }

    @Test
    public void test() throws Exception {
        pool.preparePool();
        assertThat(pool.getNumIdle(), is(3));
        Channel channel1 = pool.borrowObject();
        Channel channel2 = pool.borrowObject();
        assertThat(pool.getNumActive(), is(2));
        Channel channel3 = pool.borrowObject();
        pool.returnObject(channel1);
        pool.returnObject(channel2);
        pool.returnObject(channel3);
        assertThat(pool.getNumIdle(), is(3));
    }

}
