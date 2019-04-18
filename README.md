[![Build Status](https://travis-ci.org/baidu/brpc-java.svg?branch=master)](https://travis-ci.org/baidu/brpc-java) 
![license](https://img.shields.io/github/license/baidu/brpc-java.svg)
![maven](https://img.shields.io/maven-central/v/com.baidu/brpc-java.svg)

# 项目名称
brpc-java是baidu rpc的java版本实现，主要用于java系统中的rpc交互，支持baidu rpc、nshead、sofa、hulu、http等协议。

# 核心功能点
* 支持baidu rpc标准协议、sofa协议、hulu协议、nshead+protobuf协议、http+protobuf/json协议。
* 可以灵活自定义任意协议，只需要实现Protocol接口，客户端和服务端可以分开实现。
* 支持使用POJO替代protobuf生成的类来进行序列化（基于[jprotobuf](https://github.com/jhunters/jprotobuf)实现）。
* 支持多种naming服务，比如zookeeper、List、File、DNS等，可以灵活扩展支持etcd、eureka、nacos等。
* 支持多种负载均衡策略，比如fair、random、round robin、weight等。
* 支持interceptor功能。
* 支持server端限流：计数器、令牌桶等算法。
* 支持snappy、gzip、zlib压缩。
* 将RPC功能和Spring功能分离，既适用于一些无需spring的场景，也适用于包含Spring的场景。

## 快速开始
### 开发环境
java 6+ && netty 4 && protobuf 2.5.0

### 引入maven依赖
非Spring环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java</artifactId>
    <version>2.2.1</version>
</dependency>
```
Spring环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-spring</artifactId>
    <version>2.2.1</version>
</dependency>
```
Zookeeper注册中心：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-naming-zookeeper</artifactId>
    <version>2.2.1</version>
</dependency>
```
Consul注册中心：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-naming-consul</artifactId>
    <version>2.2.1</version>
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

### 服务注册发现
* [Zookeeper注册中心](https://github.com/baidu/brpc-java/blob/master/docs/cn/zookeeper.md)

### 扩展
* [新增协议](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增服务发现](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增负载均衡](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增压缩算法](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增拦截器](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增限流算法](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)

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


