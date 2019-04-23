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
package com.baidu.brpc.server;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.utils.ThreadPool;
import java.util.Map;

import static com.baidu.brpc.server.HttpConstants.BOLD_FONT;
import static com.baidu.brpc.server.HttpConstants.BOLD_FONT_END;
import static com.baidu.brpc.server.HttpConstants.HTML_HEAD;
import static com.baidu.brpc.server.HttpConstants.LINE_BREAK;
import static com.baidu.brpc.server.HttpConstants.PRE_STARTS;

/**
 * Server status.
 *
 * @author xiemalin
 * @since 3.1.0
 */
public class ServerStatus {
    /** The Constant SECONDS_IN_HOUR. */
    private static final int SECONDS_IN_HOUR = 3600;

    /** The Constant SECONDS_IN_DAY. */
    private static final int SECONDS_IN_DAY = 86400;

    /** The start time. */
    private long startTime;

    /** The rpc server. */
    private RpcServer rpcServer;

    public ServerStatus(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
        this.startTime = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(HTML_HEAD);
        ret.append("Server online: ").append(getOnlineDuration(startTime)).append(LINE_BREAK);
        ret.append("RPC port:").append(rpcServer.getPort()).append(LINE_BREAK);

        ret.append("Http management port:").append(rpcServer.getPort()).append(LINE_BREAK);
        ret.append("Compress enabled(Gzip Snappy)").append(LINE_BREAK);
        ret.append("Attachment enabled").append(LINE_BREAK);

        ThreadPool.StatInfo threadPoolInfo = rpcServer.getThreadPool().getStatInfo();
        ret.append("--------------Thread status----------------").append(LINE_BREAK);
        ret.append("Thread count:").append(threadPoolInfo.getThreadNum()).append(LINE_BREAK);
        ret.append("Queue capacity:").append(threadPoolInfo.getDefaultQueueCapacity()).append(LINE_BREAK);
        ret.append("Producer queue size:").append(threadPoolInfo.getProducerQueueSize()).append(LINE_BREAK);
        ret.append("Consumer queue size:").append(threadPoolInfo.getConsumerQueueSize()).append(LINE_BREAK);
        ret.append(LINE_BREAK).append(LINE_BREAK);

        ret.append(PRE_STARTS);
        ret.append("--------------properties info(").append(RpcServerOptions.class.getDeclaredFields().length)
                .append(")----------------").append(LINE_BREAK);
        ret.append(rpcServer.getRpcServerOptions());

        ret.append(LINE_BREAK).append(LINE_BREAK);

        ServiceManager serviceManager = ServiceManager.getInstance();
        Map<String, RpcMethodInfo> serviceMap = serviceManager.getServiceMap();
        ret.append("--------------RPC service list(").append(serviceMap.size()).append(") ----------------")
                .append(LINE_BREAK);

        for (Map.Entry<String, RpcMethodInfo> entry : serviceMap.entrySet()) {
            ret.append(BOLD_FONT).append("Service name:").append(entry.getValue().getServiceName())
                    .append(LINE_BREAK);
            ret.append("Method name:").append(entry.getValue().getMethodName()).append(BOLD_FONT_END)
                    .append(LINE_BREAK);

            ret.append("Request IDL:").append(LINE_BREAK).append(
                    ((Class) entry.getValue().getInputClasses()[0]).getName())
                    .append(LINE_BREAK);
            ret.append("Response IDL:").append(LINE_BREAK).append(
                    ((Class) entry.getValue().getOutputClass()).getName())
                    .append(LINE_BREAK);

            ret.append(LINE_BREAK);
        }

        ret.append(LINE_BREAK).append(LINE_BREAK);

        return ret.toString();
    }

    /**
     * Gets the online duration.
     *
     * @param startTime the start time
     * @return the online duration
     */
    private String getOnlineDuration(long startTime) {
        StringBuilder ret = new StringBuilder();
        long ms = (System.currentTimeMillis() - startTime) / 1000;

        long days = ms / SECONDS_IN_DAY;
        long hours = (ms % SECONDS_IN_DAY) / SECONDS_IN_HOUR;
        long seconds = ((ms % SECONDS_IN_DAY) % SECONDS_IN_HOUR);

        ret.append(days).append(" days ").append(hours).append(" hours ").append(seconds).append(" seconds");

        return ret.toString();
    }

}
