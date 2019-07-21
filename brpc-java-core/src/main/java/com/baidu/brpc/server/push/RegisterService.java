package com.baidu.brpc.server.push;

/**
 * 用于serverpush 注册client
 */
public interface RegisterService {

    /**
     * 注册和保存clientName
     *
     * @param clientName
     */
    Response registerClient(String clientName);

}
