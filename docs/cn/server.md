### 示例程序

- 标准协议/hulu协议/sofa协议：RpcServerTest.java
- nshead协议：RpcServerOfProtobufTest.java
- http协议：RpcServerTest.java

### 定义接口类以及实现类

- 标准协议/hulu协议/sofa协议
- nshead协议
- http协议（包括baidu json rpc协议）

### 定义Main类
server端启动主要分两步：

- 配置RpcServerOptions：配置io线程数、工作线程数、是否是http请求等。
- 初始化RpcServer实例：所需参数为端口、RpcServerOptions（可选）、以及interceptor（可选）、NamingService（可选）。
- 注册服务实例。

具体代码如下：

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
    
### 限流
server通过CurrentLimitInterceptor实现限流，目前支持两种限流算法：

- 计数器算法：CounterCurrentLimiter.java
- 令牌桶算法：TokenBucketCurrentLimiter.java

如果不能满足需求，业务也可以实现自己的限流算法，只需实现CurrentLimiter.java这个接口即可。

启用限流功能代码示例：

    RpcServer server = new RpcServer(8000); 
    server.getInterceptors().add(new CurrentLimitInterceptor(new TokenBucketCurrentLimiter(500, 500)));