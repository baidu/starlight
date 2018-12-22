## 示例程序

- [标准协议/hulu协议/sofa协议](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/standard/RpcServerTest.java)
- [nshead协议](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/nshead/RpcServerTest.java)
- [http协议](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/http/proto/RpcServerTest.java)

## 定义接口类以及实现类

- [标准协议/hulu协议/sofa协议](https://github.com/baidu/brpc-java/blob/master/docs/cn/brpc_server.md)
- [nshead协议](https://github.com/baidu/brpc-java/blob/master/docs/cn/nshead_server.md)
- [http协议](https://github.com/baidu/brpc-java/blob/master/docs/cn/http_server.md)

## 定义Main类
server端启动主要分两步：

- 配置RpcServerOptions：配置io线程数、工作线程数、是否是http请求等。
- 初始化RpcServer实例：所需参数为端口、RpcServerOptions（可选）、以及interceptor（可选）、NamingService（可选）。
- 注册服务实例。

具体代码如下：

```java
public class RpcServerTest {
    public static void main(String[] args) throws InterruptedException {
        int port = 8002;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }
 
        RpcServerOptions options = new RpcServerOptions();
        options.setReceiveBufferSize(64 * 1024 * 1024);
        options.setSendBufferSize(64 * 1024 * 1024);
        final RpcServer rpcServer = new RpcServer(port, options);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();
 
        // make server keep running
        synchronized (RpcServerTest.class) {
            try {
                RpcServerTest.class.wait();
            } catch (Throwable e) {
            }
        }
    }
}
```

## 限流
server通过CurrentLimitInterceptor实现限流，目前支持两种限流算法：

- [计数器算法](https://github.com/baidu/brpc-java/blob/master/brpc-java-core/src/main/java/com/baidu/brpc/server/currentlimit/CounterCurrentLimiter.java)
- [令牌桶算法](https://github.com/baidu/brpc-java/blob/master/brpc-java-core/src/main/java/com/baidu/brpc/server/currentlimit/TokenBucketCurrentLimiter.java)

如果不能满足需求，业务也可以实现自己的限流算法，只需实现[CurrentLimiter.java](https://github.com/baidu/brpc-java/blob/master/brpc-java-core/src/main/java/com/baidu/brpc/server/currentlimit/CurrentLimiter.java)这个接口即可。

启用限流功能代码示例：

```java
RpcServer server = new RpcServer(8000); 
server.getInterceptors().add(new CurrentLimitInterceptor(new TokenBucketCurrentLimiter(500, 500)));
```