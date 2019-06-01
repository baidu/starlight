# 扩展新协议
## 总体步骤：
* 实现AbstractProtocol类的相关方法。
* 实现ProtocolFactory类，为新协议提供Factory。
* 利用spi机制，在resources/META-INF/services目录下创建名为com.baidu.brpc.protocol.ProtocolFactory的文件，
内容是Factory全路径类名。

## client和server共同需要实现的方法：
```java
/**
  * 客户端/服务端解析请求包成header+body buffer
  * @param in 输入byte buf
  * @return header+body buffer
  * @throws BadSchemaException header格式不对
  * @throws TooBigDataException body太大
  * @throws NotEnoughDataException 可读长度不够，由于粘包拆包问题。
  */
Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException;

/**
  * 该协议是否可以和其他协议共存。
  * @return true可以共存，false不能共存。
  */
boolean isCoexistence();
```

## client端需实现的方法：
```java
/**
 * 客户端序列化请求对象
 * @param request 待发送给服务端的对象
 * @throws Exception 序列化异常
 */
ByteBuf encodeRequest(Request request) throws Exception;
 
/**
 * 客户端反序列化rpc响应
 * @param packet header & body的buf
 * @param ctx netty channel context
 * @throws Exception 反序列化异常
 */
RpcResponse decodeResponse(Object packet, ChannelHandlerContext ctx) throws Exception;

/**
  * 连接归还的时机
  * @return true代表请求发送后立即归还连接，false表示收到响应后归还。
  */
boolean returnChannelBeforeResponse();
```

## server端需要实现的方法：
```java
/**
 * 服务端反序列化rpc请求
 * @param packet header & body的buf
 * @return 输出对象
 */
Request decodeRequest(Object packet) throws Exception;
 
/**
 * 服务端序列化返回结果。
 * @param response 服务端要返回给客户端的对象
 * @throws Exception 序列化异常
 */
ByteBuf encodeResponse(Request request, Response response) throws Exception;
```

## 扩展新的服务注册发现
* 实现NamingService接口。
* 实现NamingServiceFactory接口。
* 利用spi机制，在resources/META-INF/services目录下创建名为com.baidu.brpc.naming.NamingServiceFactory的文件，
  内容是Factory全路径类名。

## 新增负载均衡实现
* 实现LoadBalanceStrategy接口。
* 实现LoadBalanceFactory接口。
* 利用spi机制，在resources/META-INF/services目录下创建名为com.baidu.brpc.client.loadbalance.LoadBalanceFactory的文件，
  内容是Factory全路径类名。

## 新增拦截器
* 实现Interceptor接口
* 在初始化RpcClient或RpcServer时，传入interceptors参数中。

## 新增限流算法实现
* 实现CurrentLimiter接口。
* 创建CurrentLimitInterceptor实例。
* 在初始化RpcClient或RpcServer时，传入interceptors参数中。
