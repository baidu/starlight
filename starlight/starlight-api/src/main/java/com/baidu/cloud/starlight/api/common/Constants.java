/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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
 
package com.baidu.cloud.starlight.api.common;

import com.baidu.cloud.starlight.api.utils.EnvUtils;

import java.util.regex.Pattern;

/**
 * Created by liuruisen on 2019/12/3.
 */
public class Constants {

    /**** 服务描述和引用 相关参数 ****/
    /**
     * URI里指定默认值,比如有key,那么DEFAULT_KEY_PREFIX+key指定的值就是该key的默认值
     */
    public static final String DEFAULT_KEY_PREFIX = "default.";

    /**
     * Comma split pattern
     */
    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    /**
     * Group key
     */
    public static final String GROUP_KEY = "group";

    /**
     * Version key
     */
    public static final String VERSION_KEY = "version";

    /**
     * class.getCanonicalName()
     */
    public static final String INTERFACE_KEY = "interface";

    /**
     * Rpc Service Key
     */
    public static final String SERVICE_KEY = "service";

    /**
     * Connect Timeout key
     */
    public static final String CONNECT_TIMEOUT_KEY = "connect_timeout";

    /**
     * Connect Timeout value: millions
     */
    public static final Integer CONNECT_TIMEOUT_VALUE = 3000;

    /**
     * Write to remote timeout
     */
    public static final String WRITE_TIMEOUT_KEY = "write_timeout";

    /**
     * Write to remote timeout: millions
     */
    public static final Integer WRITE_TIMEOUT_VALUE = 3000;

    /**
     * Request timeout key
     */
    public static final String REQUEST_TIMEOUT_KEY = "request_timeout";

    /**
     * Request timeout value: millions， default 30s
     */
    public static final Integer REQUEST_TIMEOUT_VALUE = 30 * 1000;

    /**
     * Whether to open the connection keep-alive capability
     */
    public static final String CONNECT_KEEPALIVE_ENABLED_KEY = "connect_keepalive_enabled";

    /**
     * Default is false
     */
    public static final boolean CONNECT_KEEPALIVE_ENABLED_VALUE = false;

    /**
     * Read Idle timeout key, used in Client Side
     */
    public static final String READ_IDLE_TIMEOUT_KEY = "read_idle_timeout";

    /**
     * Read Idle timeout value
     */
    public static final int READ_IDLE_TIMEOUT_VALUE = 60;

    /**
     * Max heartbeat times key, Client Side
     */
    public static final String MAX_HEARTBEAT_TIMES_KEY = "max_heartbeat_times";

    /**
     * Max heartbeat times
     */
    public static final int MAX_HEARTBEAT_TIMES_VALUE = 3;

    /**
     * Io Thread Num Key, Used in both side
     */
    public static final String IO_THREADS_KEY = "io_thread_num";

    /**
     * Default IO Thread Nums
     */
    public static final int DEFAULT_IO_THREADS_VALUE = EnvUtils.getCpuCores();

    /**
     * RPC Channel Type: long \ short \ pool
     */
    public static final String RPC_CHANNEL_TYPE_KEY = "channel_type";

    /**
     * Default Rpc Channel type
     */
    public static final String DEFAULT_RPC_CHANNEL_TYPE_VALUE = "long";

    /**
     * Unspecified protocol
     */
    public static final String UNSPECIFIED_PROTOCOL = "unspecified";

    /**
     * Protocol key
     */
    public static final String PROTOCOL_KEY = "protocol";

    /**
     * Accept Thread Num Key, Used in Server side
     */
    public static final String ACCEPT_THREADS_KEY = "accept_thread_num";

    /**
     * Default acceptor thread nums
     */
    public static final Integer DEFAULT_ACCEPTOR_THREAD_VALUE = 1;

    /**
     * Netty Server SO_BACKLOG
     */
    public static final Integer SO_BACKLOG = 1024;

    /**
     * Netty Server SO_LINGER
     */
    public static final Integer SO_LINGER = 5;

    /**
     * Netty Server SO_SNDBUF
     */
    public static final Integer SO_SNDBUF = 64 * 1024;

    /**
     * Netty Server SO_REVBUF
     */
    public static final Integer SO_REVBUF = 64 * 1024;

    /**
     * All Idle timeout key, used in Server Side
     */
    public static final String ALL_IDLE_TIMEOUT_KEY = "all_idle_timeout";

    /**
     * All Idle timeout value
     */
    public static final int ALL_IDLE_TIMEOUT_VALUE = READ_IDLE_TIMEOUT_VALUE * 3 + 30;

    /**
     * Trace id
     */
    public static final String TRACE_ID_KEY = "trace.id";

    /**
     * X_B3_TRACE_ID
     */
    public static final String X_B3_TRACE_ID = "X-B3-TraceId";

    /**
     * span id
     */
    public static final String SPAN_ID_KEY = "span.id";

    /**
     * X_B3_SPAN_ID
     */
    public static final String X_B3_SPAN_ID = "X-B3-SpanId";

    /**
     * parent span id
     */
    public static final String PARENT_SPAN_ID_KEY = "parent.span.id";

    /**
     * request id
     */
    public static final String REQUEST_ID_KEY = "request.id";

    /**
     * Association ID for multi-level calls
     */
    public static final String SESSION_ID_KEY = "session.id";

    /**
     * Default thread pool size
     */
    public static final Integer DEFAULT_BIZ_THREAD_POOL_SIZE = Math.min(EnvUtils.getCpuCores() + 1, 32);

    /**
     * max biz work thread num key, used in both side
     */
    public static final String MAX_BIZ_WORKER_NUM_KEY = "max_biz_work_num";

    /**
     * Default max thread pool size
     */
    public static final Integer DEFAULT_MAX_BIZ_THREAD_POOL_SIZE = 500;

    /**
     * Idle thread keep alive time, second
     */
    public static final Integer IDlE_THREAD_KEEP_ALIVE_SECOND = 60;

    /**
     * Max runnable queue size, used in thread pool
     */
    public static final Integer MAX_RUNNABLE_QUEUE_SIZE = 1024;

    /**
     * Default server filters
     */
    public static final String DEFAULT_SERVER_FILTERS = "servercontext,generic,servermonitor";

    /**
     * Default client filters
     */
    public static final String DEFAULT_CLIENT_FILTERS = "clientcontext,clientmonitor";

    /**
     * Uri filters key
     */
    public static final String FILTERS_KEY = "filters";

    /**
     * Filter name split key
     */
    public static final String FILTER_NAME_SPLIT_KEY = ",";

    /**
     * Transport factory name
     */
    public static final String DEFAULT_TRANSPORT_FACTORY_NAME = "starlight";

    /**
     * Return Success
     */
    public static final Integer SUCCESS_CODE = 200;

    /**
     * BRPC Value
     */
    public static final String BRPC_VALUE = "brpc";

    /**
     * Any host
     */
    public static final String ANYHOST_VALUE = "0.0.0.0";

    /**
     * Pooled channel max connections key
     */
    public static final String MAX_TOTAL_CONNECTIONS_KEY = "max_connections";

    /**
     * Pooled channel max connections
     */
    public static final Integer MAX_TOTAL_CONNECTIONS = 8;

    /**
     * Pooled channel max idle connections key
     */
    public static final String MAX_IDLE_CONNECTIONS_KEY = "max_idle_connections";

    /**
     * Pooled channel max idle connections
     */
    public static final Integer MAX_IDLE_CONNECTIONS = 8;

    /**
     * Pooled channel min idle connections key
     */
    public static final String MIN_IDLE_CONNECTIONS_KEY = "min_idle_connections";

    /**
     * Pooled channel min idle connections
     */
    public static final Integer MIN_IDLE_CONNECTIONS = 2;

    /**
     * Pooled channel time between eviction run mills key TODO 可配置
     */
    public static final String TIME_BETWEEN_EVICTION_RUN_MILLS_KEY = "time_between_eviction_run_mills";

    /**
     * Pooled channel time between eviction run mills TODO 可配置
     */
    public static final Integer TIME_BETWEEN_EVICTION_RUN_MILLS = 5 * 60 * 1000;

    /**
     * Generic method name
     */
    public static final String GENERIC_METHOD_NAME_PREFIX = "$invoke";

    /**
     * is Generic key
     */
    public static final String IS_GENERIC_KEY = "is_generic";

    /**
     * The Constant LOCALHOST.
     */
    public static final String LOCALHOST_VALUE = "127.0.0.1";

    /**
     * Generic Key, support Starlight Client to generic call to stargate server
     */
    public static final String GENERIC_KEY = "generic";

    /**
     * Stargate uuid, used in stargate protocol support stargate Client call to starlight server
     */
    public static final String STARGATE_UUID = "stargate.id";

    /**
     * Starlight server support protocols
     */
    public static final String SERVER_PROTOCOLS = "brpc,stargate,springrest";

    /**
     * Used to store thread classloader
     */
    public static final String LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY = "thread.classloader";

    public static final String EM_LOGIC_IDC = "EM_LOGIC_IDC";
    /**
     * Jarvis platform environment variables: EM_APP
     */
    public static final String EM_APP = "EM_APP";

    /**
     * Jarvis platform environment variables: EM_PRODUCT_LINE
     */
    public static final String EM_PRODUCT_LINE = "EM_PRODUCT_LINE";

    /**
     * Jarvis platform environment variables: EM_PLATFORM
     */
    public static final String EM_PLATFORM = "EM_PLATFORM";

    /**
     * Jarvis platform environment variables: EM_CPU_CORES
     */
    public static final String EM_CPU_CORES = "EM_CPU_CORES";

    /**
     * Jarvis platform environment variables: EM_THREAD_NUMBER
     */
    public static final String EM_THREAD_NUMBER = "EM_THREAD_NUMBER";

    /**
     * Jarvis platform environment variables: EM_INSTANCE_ID
     */
    public static final String EM_INSTANCE_ID = "EM_INSTANCE_ID";

    public static final String EM_ENV_TYPE = "EM_ENV_TYPE";

    public static final String MATRIX_HOST_IP = "MATRIX_HOST_IP";

    public static final String EM_IP = "EM_IP";

    public static final String RECONNECTED_TIMES_KEY = "reconnected_times";

    public static final Integer MAX_RECONNECT_TIMES = 3;

    public static final Integer RECONNECT_RETRY_INTERVAL_AFTER_FAILED = 10; // 10s

    /**
     * RpcChannel protocol attribute key
     */
    public static final String PROTOCOL_ATTR_KEY = PROTOCOL_KEY;

    /**
     * Gracefully shutdown quiet period key
     */
    public static final String GRACEFULLY_SHUTDOWN_QUIET_PERIOD_KEY = "gracefully_shutdown_quiet_time";

    /**
     * Gracefully shutdown quiet period value: 2s
     */
    public static final Integer GRACEFULLY_SHUTDOWN_QUIET_PERIOD_VALUE = 2;

    /**
     * Gracefully shutdown timeout key
     */
    public static final String GRACEFULLY_SHUTDOWN_TIMEOUT_KEY = "gracefully_shutdown_timeout";

    /**
     * Gracefully shutdown timeout value: 30s
     */
    public static final Integer GRACEFULLY_SHUTDOWN_TIMEOUT_VALUE = 30;

    /**
     * If enable gracefully shutdown
     */
    public static final String GRACEFULLY_SHUTDOWN_ENABLE_KEY = "gracefully_shutdown";

    /**
     * Enable gracefully shutdown
     */
    public static final Boolean GRACEFULLY_SHUTDOWN_ENABLE = true;

    /**
     * Default compress type
     */
    public static final String COMPRESS_TYPE = "none";

    public static final String RECEIVE_BYTE_MSG_TIME_KEY = "receive_byte_msg_time";

    /**
     * BEFORE_XXXX: Maybe used to troubleshoot
     */
    public static final String BEFORE_DECODE_HEADER_TIME_KEY = "before_decode_header_time";

    public static final String DECODE_HEADER_COST = "decode_header_cost";

    public static final String BEFORE_DECODE_BODY_TIME_KEY = "before_decode_body_time";

    public static final String DECODE_BODY_COST = "decode_body_cost";

    public static final String BEFORE_ENCODE_HEADER_TIME_KEY = "before_encode_header_time";

    public static final String ENCODE_HEADER_COST = "encode_header_cost";

    public static final String BEFORE_ENCODE_BODY_TIME_KEY = "before_encode_body_time";

    public static final String ENCODE_BODY_COST = "encode_body_cost";

    /**
     * server side, before execute method
     */
    public static final String BEFORE_EXECUTE_METHOD_TIME_KEY = "before_execute_method_time";

    /**
     * server side, execute method cost
     */
    public static final String EXECUTE_METHOD_COST = "execute_method_cost";

    public static final String BEFORE_CLIENT_REQUEST_TIME_KEY = "before_client_request_time";

    public static final String CLIENT_REQUEST_COST = "client_request_cost";

    public static final String REMOTE_ADDRESS_KEY = "remote_address";

    public static final String BEFORE_THREAD_EXECUTE_TIME_KEY = "before_thread_execute_time";

    public static final String WAIT_FOR_THREAD_COST = "wait_for_thread_cost";

    public static final String BEFORE_IO_THREAD_EXECUTE_TIME_KEY = "before_io_thread_execute_time";

    /**
     * 等待IO线程执行的耗时
     */
    public static final String WAIT_FOR_IO_THREAD_COST_KEY = "wait_for_io_thread_cost";

    public static final String BEFORE_SERVER_FILTER_EXEC_TIME_KEY = "before_server_filter_exec_time";

    /**
     * Server Filter的执行耗时
     */
    public static final String SERVER_FILTER_EXEC_COST_KEY = "server_filter_exec_cost";

    /**
     * client side, before server execute time
     */
    public static final String BEFORE_SERVER_EXECUTE_TIME_KEY = "before_call_server_time";

    /**
     * server side, the time when server had handle all one request processes and return response
     */
    public static final String RETURN_RESPONSE_TIME_KEY = "end_request_time";

    /**
     * Brpc msg : meta proto2 body proto2
     */
    public static final String PROTO2_STD_MODE = "pb2-std";

    /**
     * Brpc msg : meta proto2 body preserve null
     */
    public static final String PROTO2_JAVA_MODE = "pb2-java";

    public static final String SERIALIZER_MODE_KEY = "serialize_mode";

    public static final String SERVLET_REQUEST_KEY = "http_servlet_request";

    public static final String SERVLET_RESPONSE_KEY = "http_servlet_response";

    /**
     * 集群模式下，服务端的应用名称，用于日志记录
     */
    public static final String PROVIDER_APP_NAME_KEY = "provider_app_name";

    /**
     * 集群模式下，consumer的应用名称，用于日志记录
     */
    public static final String CONSUMER_APP_NAME_KEY = "consumer_app_name";

    /**
     * Heartbeat ping
     */
    public static final String PING = "PING";

    /**
     * Heartbeat pong
     */
    public static final String PONG = "PONG";

    /**
     * Default value of heartbeat request time out :3000ms
     */
    public static final Integer HEARTBEAT_REQUEST_TIMEOUT = 3000;

    /**
     * Server side: the cost of server exec rpc request( for logging)
     */
    public static final String SERVER_EXEC_COST_KEY = "serv_exec_cost";

    /**
     * Server side: the timestamp of server recv req(for logging)
     */
    public static final String SERVER_RECEIVE_REQ_TIME_KEY = "serv_recv_req_time";

    /**
     * Netty io ratio key
     */
    public static final String NETTY_IO_RATIO_KEY = "netty_ioratio";

    /**
     * Default value of netty io ratio
     */
    public static final int DEFAULT_NETTY_IO_RATIO = 100;

    public static final String STARGATE_SESSION_ID_KEY = "stargate.sid";

    public static final String STARGATE_REQUEST_ID_KEY = "stargate.rid";
}
