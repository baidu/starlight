![Build Status](https://img.shields.io/github/workflow/status/baidu/brpc-java/Unit%20Test)
![Coverage](https://img.shields.io/codecov/c/gh/baidu/brpc-java)
![License](https://img.shields.io/github/license/baidu/brpc-java.svg)
![Maven](https://img.shields.io/maven-central/v/com.baidu/brpc-java.svg)

# 项目名称
brpc-java 是 baidu rpc 的 java 版本实现，支持 baidu rpc、nshead、sofa、hulu、http、stargate、dubbo 等协议。

# 核心功能点
* 支持 baidu rpc 标准协议、sofa 协议、hulu 协议、nshead+protobuf 协议、http+protobuf/json 协议、public pbrpc、stargate、dubbo协议。
* 支持 Spring Boot Starter，也支持 Spring Cloud 的服务注册发现、用 brpc-java 替换 Feign HTTP 调用，提升性能。
* 支持 Server Push 机制，并支持扩展 Server Push 协议。
* 支持多种 naming 服务，比如 Zookeeper、Consul、List、File、DNS 等，可以灵活扩展支持 etcd、eureka、nacos 等。
* 支持多种负载均衡策略，比如 fair、random、round robin、weight 等。
* 支持 interceptor 功能，支持计数器、令牌桶等 server 端限流算法。
* RPC 功能可独立使用，不是必须依赖 Spring 和注册中心功能。
* 基于 SPI 机制可灵活扩展 Protocol、NamingService 和 LoadBalance。

## 快速开始
### 开发环境
java 6+ && protobuf 2.5.0+

### 引入 maven 依赖
#### protobuf 2.x 环境
非 Spring 环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java</artifactId>
    <version>3.0.2</version>
</dependency>
```
Spring 环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-spring</artifactId>
    <version>3.0.2</version>
</dependency>
```
Spring Boot 环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-spring-boot-starter</artifactId>
    <version>3.0.2</version>
</dependency>
```
Spring Cloud 环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>spring-cloud-brpc</artifactId>
    <version>3.0.2</version>
</dependency>
```
Zookeeper 注册中心：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-naming-zookeeper</artifactId>
    <version>3.0.2</version>
</dependency>
```
Consul 注册中心：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-naming-consul</artifactId>
    <version>3.0.2</version>
</dependency>
```
#### protobuf 3.x 环境
除了引入 protobuf 2.x 环境所需依赖外，还需要增加 protobuf 3.x 依赖：
```xml
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.11.0</version>
</dependency>
```
```xml
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java-util</artifactId>
    <version>3.11.0</version>
</dependency>
```
### Server 端使用
* [Server 端基本用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/server.md)
* [搭建标准协议/sofa 协议/hulu 协议 server](https://github.com/baidu/brpc-java/blob/master/docs/cn/brpc_server.md)
* [搭建 nshead server](https://github.com/baidu/brpc-java/blob/master/docs/cn/nshead_server.md)
* [搭建 HTTP server](https://github.com/baidu/brpc-java/blob/master/docs/cn/http_server.md)
* [Server Push 推送用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/server_push.md)

### Client 端使用
* [Client 端基本用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/client.md)

### 与 Spring 集成
* [Spring 集成使用](https://github.com/baidu/brpc-java/blob/master/docs/cn/spring.md)

### 扩展
* [扩展 Protocol、NamingService、LoadBalance](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)

### 一些设计
#### 网络模型
采用 Netty 的 Reactor 网络模型，但跟常规用法有些不同：
* 没有使用 Netty 的 ByteToMessageDecoder 去解析协议，因为 ByteToMessageDecoder 内部会对 buffer 进行拷贝。
* 为了提高并发，尽量少在 IO 线程中执行业务逻辑，所以在 IO 线程中只会去解析协议的 Header 部分，并把 body 的 buffer retain 出来，然后丢给工作线程去处理；工作线程会 decode body，并执行具体业务逻辑。
* 由于粘包/拆包问题，可能一次 socket 读操作会包含多个包，所以支持了批量往工作线程中 submit 任务。

#### 零拷贝 Buffer
* [DynamicCompositeByteBuf](https://github.com/baidu/brpc-java/blob/master/docs/cn/composite_buffer.md)

#### 线程池 ThreadPool
* 调研过 JDK 的 ThreadPoolExecutor、ConcurrentLinkedQueue 以及 Disruptor，最后使用更高性能的 [ThreadPool](https://github.com/baidu/brpc-java/blob/master/brpc-java-communication/src/main/java/com/baidu/brpc/utils/ThreadPool.java) 。
* ThreadPool 内部把生产者队列、消费者队列分开，用两个锁去控制同步，当 consumer queue 为空时，且 producer queue 不为空条件满足时，会交换两个队列。

#### 比ConcurrentHashMap 更快的 FastFutureStore
* [FastFutureStore](https://github.com/baidu/brpc-java/blob/master/docs/cn/fastfuturestore.md)

## 压力测试数据
### 部署环境：
* Client/Server 机器配置：CPU 12核，内存 132G，千兆网卡。
* [压测代码](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/standard/BenchmarkTest.java)
### 压力测试结果：
| 数据量 | 5 byte | 1k byte | 2k byte | 4k byte |
|:-----:| :-----: | :-------: | :-------: | :-------: |
| QPS | 220k | 100k | 53k | 27k |

# 微信交流群：
<a href="https://brpc-java.cdn.bcebos.com/qrcode.jpeg"><img src="https://brpc-java.cdn.bcebos.com/qrcode.jpeg" width="320" /></a>
