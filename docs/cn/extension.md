## 新增协议
主要通过实现AbstractProtocol类的相关方法来支持新协议。

## 新增协议client端实现
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
```

## 新增协议server端实现
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

## 新增服务发现实现
实现NamingService接口。

## 新增负载均衡实现
实现LoadBalanceStrategy接口。

## 新增压缩算法实现
实现Compress接口。

## 新增拦截器
实现Interceptor接口

## 新增限流算法实现
实现CurrentLimiter接口。
