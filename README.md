# 项目名称
brpc-java是baidu rpc的java版本实现，主要用于java系统中的rpc交互，支持baidu rpc、nshead、sofa、hulu、http等协议。

# 核心功能点
* 支持baidu rpc标准协议、sofa协议、hulu协议、nshead+protobuf协议、http+protobuf/json协议。
* 可以灵活自定义任意协议，只需要实现Protocol接口，客户端和服务端可以分开实现。
* 支持使用POJO替代protobuf生成的类来进行序列化（基于jprotobuf实现）。
* 支持多种naming服务，比如HTTP、File、List等，可以灵活扩展支持zookeeper、etcd、nacos、eureka等。
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
    <version>2.0.0</version>
</dependency>
```
Spring环境：
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-spring</artifactId>
    <version>2.0.0</version>
</dependency>
```
### Server端使用
* [server端基本用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/server.md)
* [搭建标准协议/sofa协议/hulu协议server](https://github.com/baidu/brpc-java/blob/master/docs/cn/brpc_server.md)
* [搭建nshead server](https://github.com/baidu/brpc-java/blob/master/docs/cn/nshead_server.md)
* [搭建http server](https://github.com/baidu/brpc-java/blob/master/docs/cn/http_server.md)

### Client端使用
* [client端基本用法](https://github.com/baidu/brpc-java/blob/master/docs/cn/client.md)

### 扩展
* [新增协议](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增服务发现](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增负载均衡](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增压缩算法](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增拦截器](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)
* [新增限流算法](https://github.com/baidu/brpc-java/blob/master/docs/cn/extension.md)

### 核心设计
* [DynamicCompositeByteBuf](https://github.com/baidu/brpc-java/blob/master/docs/cn/composite_buffer.md)

## 压力测试数据
### 部署环境：
* Client/Server机器配置：cpu 12核，内存132G，千兆网卡。
* [压测代码](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/standard/BenchmarkTest.java)
### 压力测试结果：
| 数量量 | 5 byte | 1k byte | 2k byte | 4k byte |
|:-----:| :-----: | :-------: | :-------: | :-------: |
|qps    | 22w   |    10w  |  5.3w   |   2.7w  |

## 测试
mvn clean install


