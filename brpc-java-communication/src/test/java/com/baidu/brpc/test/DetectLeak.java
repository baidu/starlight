package com.baidu.brpc.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable memory leak detection on {@link io.netty.buffer.PooledByteBufAllocator}.
 * <p>
 * This should be used with {@link MemoryLeakDetectionRule}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DetectLeak {
}
