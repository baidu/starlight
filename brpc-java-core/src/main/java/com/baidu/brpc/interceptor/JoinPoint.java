package com.baidu.brpc.interceptor;

import com.baidu.brpc.protocol.Request;

/**
 * stand for intercepted join point
 * @author Li Yuanxin(liyuanxin999@163.com)
 */
public interface JoinPoint {
    /**
     * get the request
     * @return request object
     */
    Request getRequest();

    /**
     * invoke the intercepted method or interceptors around
     * @return the result returned by intercepted method
     * @throws Exception exception thrown in proceed
     */
    Object proceed() throws Exception;

    /**
     * proceed using new arguments
     * @param args new arguments
     * @return the result returned by intercepted method
     * @throws Exception exception thrown in proceed
     */
    Object proceed(Object[] args) throws Exception;
}
