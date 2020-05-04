package com.baidu.brpc.protocol.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.baidu.brpc.test.DetectLeak;
import com.baidu.brpc.test.MemoryLeakDetectionRule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;

@Slf4j
public class BrpcHttpObjectDecoderTest {

    @DetectLeak
    protected PooledByteBufAllocator alloc;
    @Rule
    public MemoryLeakDetectionRule memoryLeakDetectionRule = new MemoryLeakDetectionRule(this);

    @Test
    public void testDecodePartial() throws Exception {
        BrpcHttpObjectDecoder decoder = BrpcHttpObjectDecoder.getDecoder(true);
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        when(ctx.alloc()).thenReturn(alloc);
        ByteBuf buf = alloc.buffer(1024);
        String[] testRequest = new String[]{
                "GET / HTTP/1.1",
                "Host: localhost",
                "Content-Length: 4096",
                "",
                "abc"
        }; // partial request
        buf.writeBytes(StringUtils.join(testRequest, "\n\r").getBytes(Charset.forName("UTF-8")));
        Object message = decoder.decode(ctx, buf);
        assertThat(message).isNull();
        ReferenceCountUtil.release(buf);
    }

    @Test
    public void testDecode() throws Exception {
        BrpcHttpObjectDecoder decoder = BrpcHttpObjectDecoder.getDecoder(true);
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        when(ctx.alloc()).thenReturn(alloc);
        ByteBuf buf = alloc.buffer(1024);
        String[] testRequest = new String[]{
                "GET / HTTP/1.1",
                "Host: localhost",
                "Content-Length: 10",
                "",
                "1234567890"
        }; // full request
        buf.writeBytes(StringUtils.join(testRequest, "\n\r").getBytes(Charset.forName("UTF-8")));
        Object message = decoder.decode(ctx, buf);
        assertThat(message).isNotNull();
        ReferenceCountUtil.release(buf);
        ReferenceCountUtil.release(message);
    }

}