## 标准协议/hulu协议/sofa协议

示例程序

```java
brpc-java-examples/src/main/java/com/baidu/brpc/example/standard/RpcServerTest.java
```

定义请求和响应结构proto

```java
package example;
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
    
也支持使用jprotobuf方式定义的普通java类作为request/response，具体使用例子请见:

```java
src/test/java/io/brpc/example/jprotobuf
```

定义java接口

```java
public interface EchoService {
    @BrpcMeta(serviceName = "example.EchoService", methodName = "Echo")
    Echo.EchoResponse echo(Echo.EchoRequest request);
}
```
    
在java实现中，接口类并没有使用proto生成的EchoService，而是由业务自己去定义接口类，原因有两点：

- 为了多协议统一而做的通用设计，比如非proto协议场景。
- 自定义的接口类，不用依赖controller对象，更符合RPC习惯。

BrpcMeta注解标记在接口类方法上，使用场景是 当java与c++通信时，java package与c++ namespace命名习惯不同，可以使用该注解来定制。BrpcMeta有两个属性：

- 对于标准协议，serviceName默认是"包名"."类名"，methodName是proto文件Service内对应方法名；
- 对于hulu协议，serviceName默认是类名，methodName是proto文件Service内对应方法index，index默认从0开始。

如果是java client和java server通信，BrpcMeta可以不填。当BrpcMeta不设置时，框架使用接口全名作为serviceName，方法名作为methodName。

接口实现类

```java
public class EchoServiceImpl implements EchoService {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServiceImpl.class);
 
    @Override
    public Echo.EchoResponse echo(Echo.EchoRequest request) {
        String message = request.getMessage();
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                .setMessage(message).build();
        LOG.debug("EchoService.echo, request={}, response={}",
                request.getMessage(), response.getMessage());
        return response;
    }
}
```
    
对于标准协议和hulu协议有attachment功能，使用方式如下：

```java
// 读取request attachment方法
RpcContext rpcContext = RpcContext.getContext();
ByteBuf attachment = rpcContext.getRequestBinaryAttachment();
// 设置response attachment方法
rpcContext.setResponseBinaryAttachment("hello".getBytes());
```
 
server端request attachment和response attachment的byteBuf由框架去release，业务不需要手动释放。