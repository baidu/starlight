[![Build Status](https://travis-ci.org/baidu/brpc-java.svg?branch=master)](https://travis-ci.org/baidu/brpc-java) 
![license](https://img.shields.io/github/license/baidu/brpc-java.svg)
![maven](https://img.shields.io/maven-central/v/com.baidu/brpc-java.svg)

# 项目名称
brpc-java是baidu rpc的java版本实现，支持baidu rpc、nshead、sofa、hulu、http、stargate等协议。

# 核心功能点
* 支持baidu rpc标准协议、sofa协议、hulu协议、nshead+protobuf协议、http+protobuf/json协议、public pbrpc、stargate协议。
* 支持SpringBoot starter，也支持SpringCloud的服务注册发现、用brpc-java替换Feign http调用，提升性能。
* 支持多种naming服务，比如Zookeeper、Consul、List、File、DNS等，可以灵活扩展支持etcd、eureka、nacos等。
* 支持多种负载均衡策略，比如fair、random、round robin、weight等。
* 支持interceptor功能，支持计数器、令牌桶等server端限流算法。
* rpc功能可独立使用，不是必须依赖Spring和注册中心功能。
* 基于SPI机制可灵活扩展Protocol、NamingService和LoadBalance。

## 快速开始
### 开发环境
java 6+ && netty 4 && protobuf 2.5.0

### 引入maven依赖
非Spring环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java</artifactId>
    <version>2.4.3</version>
</dependency>
```
Spring环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-spring</artifactId>
    <version>2.4.3</version>
</dependency>
```
SpringBoot环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-spring-boot-stater</artifactId>
    <version>2.4.3</version>
</dependency>
```
SpringCloud环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>spring-cloud-brpc</artifactId>
    <version>2.4.3</version>
</dependency>
```
Zookeeper注册中心：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-naming-zookeeper</artifactId>
    <version>2.4.3</version>
</dependency>
```
Consul注册中心：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-naming-consul</artifactId>
    <version>2.4.3</version>
</dependency>
```
### Server端使用
* [server端基本用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/server.md)
* [搭建标准协议/sofa协议/hulu协议server](https://github.com/baidu/brpc-java/blob/master/docs/cn/brpc_server.md)
* [搭建nshead server](https://github.com/baidu/brpc-java/blob/master/docs/cn/nshead_server.md)
* [搭建http server](https://github.com/baidu/brpc-java/blob/master/docs/cn/http_server.md)

### Client端使用
* [client端基本用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/client.md)

### 与Spring集成
* [Spring集成使用](https://github.com/baidu/brpc-java/blob/master/docs/cn/spring.md)

### 扩展
* [扩展Protocol、NamingService、LoadBalance](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)

### 一些设计
#### 网络模型
采用netty的reactor网络模型，但跟常规用法有些不同：
* 没有使用netty的ByteToMessageDecoder去解析协议，因为ByteToMessageDecoder内部会对buffer进行拷贝。
* 为了提高并发，尽量少在IO线程中执行业务逻辑，所以在io线程中只会去解析协议的header部分，并把body的buffer retain出来，然后丢给工作线程去处理；工作线程会decode body，并执行具体业务逻辑。
* 由于粘包/拆包问题，可能一次socket读操作会包含多个包，所以支持了批量往工作线程中submit任务。

#### 零拷贝Buffer
* [DynamicCompositeByteBuf](https://github.com/baidu/brpc-java/blob/master/docs/cn/composite_buffer.md)

#### 线程池ThreadPool
* 调研过JDK的ThreadPoolExecutor、ConcurrentLinkedQueue以及Disruptor，最后使用更高性能的[ThreadPool](
https://github.com/baidu/brpc-java/blob/master/brpc-java-core/src/main/java/com/baidu/brpc/utils/ThreadPool.java)。
* ThreadPool内部把生产者队列、消费者队列分开，用两个锁去控制同步，当consumer queue为空时，且producer queue不为空条件满足时，会交换两个队列。

#### 比ConcurrentHashMap更快的FastFutureStore
* [FastFutureStore](https://github.com/baidu/brpc-java/blob/master/docs/cn/fastfuturestore.md)

## 压力测试数据
### 部署环境：
* Client/Server机器配置：cpu 12核，内存132G，千兆网卡。
* [压测代码](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/standard/BenchmarkTest.java)
### 压力测试结果：
| 数据量 | 5 byte | 1k byte | 2k byte | 4k byte |
|:-----:| :-----: | :-------: | :-------: | :-------: |
|qps    | 22w   |    10w  |  5.3w   |   2.7w  |

# 微信交流群：
<img src="https://github.com/baidu/brpc-java/blob/master/weixin_qrcode.png" width=200 height=300 />


