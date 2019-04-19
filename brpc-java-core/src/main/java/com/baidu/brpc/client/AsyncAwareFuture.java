package com.baidu.brpc.client;

import java.util.concurrent.Future;

/**
 * Created by wanghongfei on 2019-04-19.
 */
public interface AsyncAwareFuture<T> extends Future<T> {
    boolean isAsync();
}
