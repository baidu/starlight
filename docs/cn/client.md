## 示例程序
```java
brpc-java-examples/src/main/java/com/baidu/brpc/example/standard/RpcClientTest.java
```
## 接口类定义
同server端，请见server.md

## 初始化RpcClient
RpcClient可传入四个参数：

- 服务端地址：必选，支持多种naming方式，包括list://,bns://,file://,http://
- RpcClientOptions：可选，用于设置读写超时、交互协议、工作线程个数等重要参数。
- 拦截器：可选，在发送请求和接受响应时，进行拦截。
- NamingService：命名服务。

在初始化RpcClient实例时，内部会建立连接池、创建netty acceptor线程池、io线程池、work线程池以及各种定时线程池。

所以应该在系统初始化时，就创建RpcClient实例，切勿在每次发送请求前去创建；而且对于同一个server，应该只初始化一个RpcClient实例。

RpcClient是线程安全的。

## 生成接口代理类

```java
EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
```

接口代理类是线程安全的，只需要在系统初始化时，创建一个全局实例即可。

有了接口类后，就可以像普通函数调用一样，进行rpc调用。

## 异步调用
### 1、定义异步callback

通过实现RpcCallback接口的success/fail方法进行异步回调。

如果协议支持二进制attachment字段，需要在release ResponseBinaryAttachment。

```java
RpcCallback callback = new RpcCallback<Echo.EchoResponse>() {
    @Override
    public void success(Echo.EchoResponse response) {
        System.out.printf("async call EchoService.echo success, response=%s\n",
                response.getMessage());
        if (RpcContext.getContext().getResponseBinaryAttachment() != null) {
            System.out.println("attachment="
                    + new String(RpcContext.getContext().getResponseBinaryAttachment().array()));
            ReferenceCountUtil.release(RpcContext.getContext().getResponseBinaryAttachment());
        }
    }
 
    @Override
    public void fail(Throwable e) {
        System.out.printf("async call EchoService.echo failed, %s\n", e.getMessage());
    }
};
```

### 2、定义异步接口类
```java
public interface EchoServiceAsync extends EchoService {
    Future<Echo.EchoResponse> echo(Echo.EchoRequest request, RpcCallback<Echo.EchoResponse> callback);
}
```
 
异步接口类继承同步接口类，方法名要跟同步方法名一样，但比同步方法多一个callback参数。

异步接口类只需在client端定义，server端不需要。

### 3、生成异步接口代理类
```java
EchoServiceAsync asyncEchoService = RpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
```

异步和同步可共用一个RpcClient实例。

异步调用跟同步不一样，异步调用返回Future对象，可调用future.get()进行同步等待。

如果使用场景是，同时发送多个异步请求出去，然后要等待所有server都返回后，再执行后续业务；这时候可以通过future.get()完成。

## 连接方式

默认是使用连接池。但如果业务觉得连接池比较慢或者因业务需求想使用固定连接，可以通过如下方式手动设置连接：
```java
channel = rpcClient.selectChannel();
RpcContext.getContext().setChannel(channel);
```

对于手动设置的channel，需要业务自己去归还。正常归还时，调用
```java
rpcClient.returnChannel(channel);
```

出错时，调用
```java
rpcClient.removeChannel(channel);
```

## 负载均衡
支持以下几种负载均衡：
- RANDOM：随机
- ROUND_ROBIN：轮询
- WEIGHT：基于权重的负载均衡，该负载均衡会记录server实例的成功、失败次数，尽量选择失败次数较少的实例。
- FAIR：基于响应时间的负载均衡，该负载均衡会记录server实例平均响应时间，尽量选择响应时间端的实例，具体算法请参考GST fair 负载均衡策略

## Naming方式
支持以下几种naming方式：
- LIST：格式如"list://127.0.0.1:8002"
- BNS：格式如"bns://test.bj"
- FILE：格式如"file://conf/server_list.conf"，文件内容是一行一个ip:port
- HTTP：格式如"http://test.baidu.com/server/list"，http返回内容是一行一个ip:port