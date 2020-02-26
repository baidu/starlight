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

package com.baidu.brpc.protocol.nshead;

import com.baidu.brpc.exceptions.BadSchemaException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class NSHeadTest {

    @Test
    public void fromByteBuf() throws BadSchemaException {
        byte[] bytes = new byte[]{
                -22, 0, 1, 0, 123, 0, 0, 0, 108, 111, 110, 103, 101, 114, 45, 116, 104,
                97, 110, 45, 115, 105, 120, 116, -108, -109, 112, -5, 0, 0, 0, 0, 41, 9, 0, 0
        };
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        NSHead head = NSHead.fromByteBuf(buf);
        assertThat(head.id, equalTo((short) 234));
        assertThat(head.logId, equalTo(123));
        assertThat(head.version, equalTo((short) 1));
        assertThat(head.provider, equalTo("longer-than-sixt")); // een discard
        assertThat(head.bodyLength, equalTo(2345));
    }

    @Test
    public void toBytes() {
        byte[] bytes = new byte[]{
                -22, 0, 1, 0, 123, 0, 0, 0, 108, 111, 110, 103, 101, 114, 45, 116, 104,
                97, 110, 45, 115, 105, 120, 116, -108, -109, 112, -5, 0, 0, 0, 0, 41, 9, 0, 0
        };
        NSHead head = new NSHead(123, (short) 234, (short) 1, "longer-than-sixteen", 2345);
        byte[] serialized = head.toBytes();
        assertThat(serialized, equalTo(bytes));
    }
}