package com.baidu.brpc.thread;

import com.baidu.brpc.utils.BrpcConstants;
import com.baidu.brpc.utils.CustomThreadFactory;
import com.baidu.brpc.utils.ThreadPool;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Slf4j
public class BrpcThreadPoolManager {
    private volatile EventLoopGroup defaultIoThreadPool;
    private ConcurrentMap<String, EventLoopGroup> ioThreadPoolMap
            = new ConcurrentHashMap<String, EventLoopGroup>();
    private volatile ThreadPool defaultWorkThreadPool;
    private ConcurrentMap<String, ThreadPool> workThreadPoolMap
            = new ConcurrentHashMap<String, ThreadPool>();
    private ExecutorService exceptionThreadPool = Executors.newFixedThreadPool(
            1, new CustomThreadFactory("exception-thread"));

    private static volatile BrpcThreadPoolManager instance;

    public static BrpcThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized(BrpcThreadPoolManager.class) {
                if (instance == null) {
                    instance = new BrpcThreadPoolManager();
                }
            }
        }
        return instance;
    }

    public EventLoopGroup getOrCreateClientIoThreadPool(String serviceName, boolean isSharing,
                                                  int threadNum, int ioEventType) {
        if (isSharing) {
            if (defaultIoThreadPool == null) {
                synchronized (BrpcThreadPoolManager.class) {
                    if (defaultIoThreadPool == null) {
                        defaultIoThreadPool = createClientIoThreadPool(
                                threadNum, "brpc-client-io-thread-default", ioEventType);
                    }
                }
            }
            return defaultIoThreadPool;
        }

        EventLoopGroup threadPool;
        if ((threadPool = ioThreadPoolMap.get(serviceName)) == null) {
            synchronized (serviceName.intern()) {
                if ((threadPool = ioThreadPoolMap.get(serviceName)) == null) {
                    threadPool = createClientIoThreadPool(
                            threadNum, "brpc-client-io-thread-" + serviceName, ioEventType);
                    EventLoopGroup prev = ioThreadPoolMap.putIfAbsent(serviceName, threadPool);
                    if (prev != null) {
                        log.warn("brpc io thread pool exist for service:{}", serviceName);
                        threadPool.shutdownGracefully().awaitUninterruptibly();
                    }
                }
            }
        }
        return threadPool;
    }

    protected EventLoopGroup createClientIoThreadPool(int threadNum, String namePrefix, int ioEventType) {
        EventLoopGroup eventLoopGroup;
        if (ioEventType == BrpcConstants.IO_EVENT_NETTY_EPOLL) {
            eventLoopGroup = new EpollEventLoopGroup(threadNum,
                    new CustomThreadFactory(namePrefix));
        } else {
            eventLoopGroup = new NioEventLoopGroup(threadNum,
                    new CustomThreadFactory(namePrefix));
        }
        return eventLoopGroup;
    }

    public ThreadPool getOrCreateClientWorkThreadPool(String serviceName, boolean isSharing, int threadNum) {
        if (isSharing) {
            if (defaultWorkThreadPool == null) {
                synchronized (BrpcThreadPoolManager.class) {
                    if (defaultWorkThreadPool == null) {
                        defaultWorkThreadPool = new ThreadPool(threadNum,
                                new CustomThreadFactory("brpc-client-work-thread-default"));
                    }
                }
            }
            return defaultWorkThreadPool;
        }

        ThreadPool threadPool;
        if ((threadPool = workThreadPoolMap.get(serviceName)) == null) {
            synchronized (serviceName.intern()) {
                if ((threadPool = workThreadPoolMap.get(serviceName)) == null) {
                    threadPool = new ThreadPool(threadNum,
                            new CustomThreadFactory("brpc-client-work-thread-" + serviceName));
                    workThreadPoolMap.put(serviceName, threadPool);
                }
            }
        }
        return threadPool;
    }

    public void stopAll() {
        Iterator<Map.Entry<String, EventLoopGroup>> iterator = ioThreadPoolMap.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().shutdownGracefully().awaitUninterruptibly();
            iterator.remove();
        }

        Iterator<Map.Entry<String, ThreadPool>> iterator2 = workThreadPoolMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            iterator2.next().getValue().stop();
            iterator2.remove();
        }
        if (defaultIoThreadPool != null) {
            defaultIoThreadPool.shutdownGracefully().awaitUninterruptibly();
            defaultIoThreadPool = null;
        }
        if (exceptionThreadPool != null) {
            exceptionThreadPool.shutdownNow();
        }
    }

    public void stopServiceThreadPool(String serviceName) {
        BrpcThreadPoolManager threadPoolManager = BrpcThreadPoolManager.getInstance();
        EventLoopGroup ioThreadPool = threadPoolManager.getIoThreadPoolMap().remove(serviceName);
        if (ioThreadPool != null) {
            ioThreadPool.shutdownGracefully().syncUninterruptibly();
        }
        ThreadPool workThreadPool = threadPoolManager.getWorkThreadPoolMap().remove(serviceName);
        if (workThreadPool != null) {
            workThreadPool.stop();
        }
    }

}
