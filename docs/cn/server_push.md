## 服务端推送（server push）协议
服务端推送功能的主要原理为， 在客户端启动时会向服务端注册clientName并保持长连接，服务端通过clientName向指定的客户端推送消息


### 示例程序
[RpcServerTest](https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/brpc-java-core-examples/src/main/java/com/baidu/brpc/example/push/RpcServerPushTest.java   
https://github.com/baidu/brpc-java/blob/master/brpc-java-examples/brpc-java-core-examples/src/main/java/com/baidu/brpc/example/push/RpcClientPushTest.java)


### 使用默认的server push协议
client端：  
首先定义client用于接收消息接口
```java
public interface UserPushApi {

    PushResult clientReceive(String extra, PushData data);

}
```
与服务端建立连接
```java
RpcClientOptions clientOption = new RpcClientOptions();
clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);

// 指定clientName
clientOption.setClientName("c1");

// 创建客户端 c1
RpcClient rpcClient = new RpcClient(serviceUrl, clientOption);
// 首先建立一个普通rpc client服务, 与后端建立起连接
EchoService service1 = BrpcProxy.getProxy(rpcClient, EchoService.class);
// 注册实现push方法
rpcClient.registerPushService(new UserPushApiImpl());

```
 
 服务端：  
 继承client的接口，增加一个带有clientName的同名方法
 ```java
public interface ServerSideUserPushApi extends UserPushApi {
    
    // server端接口， 多一个clientName参数
    PushResult clientReceive(String clientName, String extra, PushData data);

}
 ```
 服务端向c1客户端发请求
 ```java
RpcServerOptions options = new RpcServerOptions();
options.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
rpcServer = new RpcServer(port, options);
rpcServer.registerService(new EchoServiceImpl()); // 普通rpc服务
// get push api
ServerSideUserPushApi proxyPushApi = BrpcPushProxy.getProxy(rpcServer, ServerSideUserPushApi.class);
rpcServer.start();
// send push data to c1
PushData p1 = new PushData();
PushResult pushResultI = proxyPushApi.clientReceive("c1","abc", p1);
 
 ```

### 多个client实例的情况
当多个client用相同的clientName注册到服务端的时候， 服务端推送时，只会向其中一个client发送请求


### 自定义 server push 协议
如果要自定义server push 协议实现， 可以通过实现相关接口实现   
实现ServerPushProtocol、ServerPushPacket和SPHead ， 然后通过自定义ServerPushProtocolFactory和提供com.baidu.brpc.protocol
.ProtocolFactory 文件进行加载

