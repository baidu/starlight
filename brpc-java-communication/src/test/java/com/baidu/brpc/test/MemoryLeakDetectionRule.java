package com.baidu.brpc.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class MemoryLeakDetectionRule implements TestRule {

    private final Object instance;

    public MemoryLeakDetectionRule(Object instance) {
        this.instance = instance;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        Class<?> testClass = description.getTestClass();
        final List<Field> allocators = new ArrayList<Field>();
        for (Field field : testClass.getDeclaredFields()) {
            DetectLeak detectLeak = field.getAnnotation(DetectLeak.class);
            if (detectLeak == null) {
                continue;
            }
            if (!PooledByteBufAllocator.class.equals(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            allocators.add(field);
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                setupPools(allocators);
                base.evaluate();
                checkLeaks(allocators);
            }
        };
    }

    private void setupPools(List<Field> allocators) {
        Iterator<Field> it = allocators.iterator();
        while (it.hasNext()) {
            Field field = it.next();
            PooledByteBufAllocator alloc = new PooledByteBufAllocator(true, 0, 1, 8192, 11, 0, 0, 0, false);
            try {
                field.set(instance, alloc);
            } catch (Exception ex) {
                log.warn("Failed to process field {} for memory leak detection", field.getName());
                it.remove();
            }
        }
    }

    private void checkLeaks(List<Field> allocators) {
        for (Field field : allocators) {
            PooledByteBufAllocator alloc;
            try {
                alloc = (PooledByteBufAllocator) field.get(instance);
            } catch (Exception ex) {
                log.warn("Failed to process field {} for memory leak detection", field.getName(), ex);
                continue;
            }
            assertThat(alloc).as("PooledByteBufAllocator").isNotNull();
            assertThat(getActiveHeapBuffers(alloc)).as("active heap memory").isZero();
            assertThat(getActiveDirectBuffers(alloc)).as("active direct memory").isZero();
        }
    }

    private static int getActiveDirectBuffers(PooledByteBufAllocator alloc) {
        int directActive = 0, directAlloc = 0, directDealloc = 0;
        for (PoolArenaMetric arena : alloc.metric().directArenas()) {
            directActive += arena.numActiveAllocations();
            directAlloc += arena.numAllocations();
            directDealloc += arena.numDeallocations();
        }
        log.info("direct memory usage, active: {}, alloc: {}, dealloc: {}", directActive, directAlloc, directDealloc);
        return directActive;
    }

    private static int getActiveHeapBuffers(PooledByteBufAllocator alloc) {
        int heapActive = 0, heapAlloc = 0, heapDealloc = 0;
        for (PoolArenaMetric arena : alloc.metric().heapArenas()) {
            heapActive += arena.numActiveAllocations();
            heapAlloc += arena.numAllocations();
            heapDealloc += arena.numDeallocations();
        }
        log.info("heap memory usage, active: {}, alloc: {}, dealloc: {}", heapActive, heapAlloc, heapDealloc);
        return heapActive;
    }
}
