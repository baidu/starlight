# 项目名称
brpc-java是baidu rpc的java版本实现。
主要用于java系统中的rpc交互（包括baidu rpc、nshead、sofa、hulu、baidu json rpc等协议）。

# 核心功能点
* 支持baidu rpc标准协议、sofa协议、hulu协议、nshead+mcpack/protobuf协议、http+protobuf/json协议、baidu json rpc协议。
* 可以灵活自定义任意协议，只需要实现Protocol接口，客户端和服务端可以分开实现。
* 支持使用POJO替代protobuf生成的类来进行序列化（基于jprotobuf实现）。
* 支持多种naming服务，比如HTTP、File、List等，可以灵活扩展支持zookeeper、etcd、nacos、eureka等。
* 支持多种负载均衡策略，比如fair、random、round robin、weight等。
* 支持interceptor功能。
* 支持server端限流：计数器、令牌桶等算法。
* 支持snappy、gzip、zlib压缩。
* 将RPC功能和Spring功能分离，既适用于一些无需spring的场景，也适用于基于Spring的场景。

## 快速开始
### 开发环境
java 6+ && netty 4 && protobuf 2.5.0

### 引入maven依赖
```xml
<dependency>
    <groupId>io.brpc</groupId>
    <artifactId>brpc-java</artifactId>
    <version>2.0.0</version>
</dependency>
```
### 定义请求和响应的protobuf message结构
```proto
package example_for_cpp;
option cc_generic_services = true;
option java_package="io.brpc.example.standard";
option java_outer_classname="Echo";

message EchoRequest {
      required string message = 1;
};

message EchoResponse {
      required string message = 1;
};

service EchoService {
      rpc Echo(EchoRequest) returns (EchoResponse);
};
```
### 定义java接口
```java
// 同步接口
public interface EchoService {
    /**
      * brpc/sofa：
      * serviceName默认是包名 + 类名，methodName是proto文件Service内对应方法名，
      * hulu：
      * serviceName默认是类名，methodName是proto文件Service内对应方法index。
      */
    @RpcMeta(serviceName = "example_for_cpp.EchoService", methodName = "Echo")
//    @RpcMeta(serviceName = "EchoService", methodName = "0")
    Echo.EchoResponse echo(Echo.EchoRequest request);
}
```
```java
// 异步接口
public interface EchoServiceAsync extends EchoService {
    Future<Echo.EchoResponse> echo(Echo.EchoRequest request, RpcCallback<Echo.EchoResponse> callback);
}
```
### 服务端开发
#### 接口实现类
```java
public class EchoServiceImpl implements EchoService {

    private static final Logger LOG = LoggerFactory.getLogger(EchoServiceImpl.class);

    @Override
    public Echo.EchoResponse echo(Echo.EchoRequest request) {
        // 读取request attachment
        RpcContext rpcContext = RpcContext.getContext();
        String attachmentString = "";
        // server端request binary attachment由框架去release，不需要业务主动release。
        ByteBuf attachment = rpcContext.getRequestBinaryAttachment();
        if (attachment != null) {
            attachmentString = new String(attachment.array());
        }
        
        String message = request.getMessage();
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage(message).build();
        // 设置response attachment
        // server端response binary attachment由框架去release，不需要业务主动release。
        rpcContext.setResponseBinaryAttachment(Unpooled.wrappedBuffer(attachmentString.getBytes()));
        LOG.debug("EchoService.echo, request={}, request_attachment={}, response={}, response_attachment={}",
                request.getMessage(), attachmentString, response.getMessage(), attachmentString);
        return response;
    }
}
```
#### 服务端启动类
```java
public class RpcServerTest {
    public static void main(String[] args) {
        int port = 8002;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }

        RpcServerOptions options = new RpcServerOptions();
        RpcServer rpcServer = new RpcServer(port, options, null);
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
### 客户端开发
```java
public class RpcClientTest {
    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(1000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(10);
        clientOption.setLoadBalanceType(LoadBalanceType.WEIGHT.getId());
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_SNAPPY);

        String serviceUrl = "list://127.0.0.1:8002";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();;
        interceptors.add(new CustomInterceptor());
        // 对于同一类下游服务，只需要全局初始化一个RpcClient实例；
        // 切勿在每次请求server时初始化RpcClient，因为初始化RpcClient很耗时。
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);

        // build request
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder()
                .setMessage("hello")
                .build();

        // sync call
        EchoService echoService = RpcProxy.getProxy(rpcClient, EchoService.class);
        Channel channel = null;
        try {
            // client request binary attachment不需要业务去release，框架会在发送完成后进行release。
            RpcContext.getContext().setRequestBinaryAttachment("example attachment".getBytes());
            // 可选功能：
            // 如果手动指定channel，则RpcClient使用该channel发送请求；
            // 手动指定的channel，由业务自己调用rpcClient.returnChannel归还；
            // 如果出错，由业务自己调用rpcClient.removeChannel从连接池删除。
            channel = rpcClient.selectChannel();
            RpcContext.getContext().setChannel(channel);

            Echo.EchoResponse response = echoService.echo(request);
            System.out.printf("sync call service=EchoService.echo success, "
                            + "request=%s,response=%s\n",
                    request.getMessage(), response.getMessage());
            if (RpcContext.getContext().getResponseBinaryAttachment() != null) {
                System.out.println("attachment="
                        + new String(RpcContext.getContext().getResponseBinaryAttachment().array()));
                // client response binary attachment由业务自己去release。
                ReferenceCountUtil.release(RpcContext.getContext().getResponseBinaryAttachment());
            }
        } catch (RpcException ex) {
            System.out.println("sync call failed, ex=" + ex.getMessage());
            rpcClient.removeChannel(channel);
            channel = null;
        } finally {
            if (channel != null) {
                rpcClient.returnChannel(channel);
            }
            RpcContext.removeContext();
        }

        // async call
        RpcCallback callback = new RpcCallback<Echo.EchoResponse>() {
            @Override
            public void success(Echo.EchoResponse response) {
                if (response != null) {
                    System.out.printf("async call EchoService.echo success, response=%s\n",
                            response.getMessage());
                    if (RpcContext.getContext().getResponseBinaryAttachment() != null) {
                        System.out.println("attachment="
                                + new String(RpcContext.getContext().getResponseBinaryAttachment().array()));
                        ReferenceCountUtil.release(RpcContext.getContext().getResponseBinaryAttachment());
                    }
                } else {
                    System.out.println("async call failed, service=EchoService.echo");
                }
            }

            @Override
            public void fail(Throwable e) {
                System.out.printf("async call EchoService.echo failed, %s\n", e.getMessage());
            }
        };
        EchoServiceAsync asyncEchoService = RpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
        try {
            RpcContext.getContext().setRequestBinaryAttachment("async example attachment".getBytes());
            Future<Echo.EchoResponse> future = asyncEchoService.echo(request, callback);
            try {
                if (future != null) {
                    future.get();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (RpcException ex) {
            System.out.println("rpc send failed, ex=" + ex.getMessage());
        } finally {
            RpcContext.getContext().removeContext();
        }
        rpcClient.stop();
    }
}
```

## 拦截器功能
### 拦截器使用场景较多，比如
* client端希望拦截所有请求并新增用户名、密码字段；
* server端在处理请求之前，增加用户认证功能；
* server端限流；
* server端在处理请求后，打印日志。
* client端新增trace功能。
### brpc-java拦截器使用方法
* 实现Interceptor接口：
```java
public class CustomInterceptor implements Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CustomInterceptor.class);

    public boolean handleRequest(RpcRequest rpcRequest) {
        LOG.info("request intercepted, logId={}, service={}, method={}",
                rpcRequest.getLogId(),
                rpcRequest.getTarget().getClass().getSimpleName(),
                rpcRequest.getTargetMethod().getName());
        return true;
    }

    public void handleResponse(RpcResponse response) {
        LOG.info("reponse intercepted, logId={}, result={}",
                response.getLogId(), response.getResult());
    }
}
```
* 在初始化RpcClient、RpcServer时，传入interceptors列表。

## 压力测试数据
### 部署环境：
* Client/Server机器配置：cpu 12核，内存132G，千兆网卡。
* 具体压测代码在brpc-java-examples/src/main/java/com/baidu/brpc/example/standard中。
### 压力测试结果：
| 数量量 | 5 byte | 1k byte | 2k byte | 4k byte |
|:-----:| :-----: | :-------: | :-------: | :-------: |
|qps    | 22w   |    10w  |  5.3w   |   2.7w  |

## 测试
mvn clean install


