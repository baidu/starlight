## 新增协议
主要通过实现AbstractProtocol类的相关方法来支持新协议。

## 新增TCP协议client端实现
```java
/**
 * 客户端/服务端解析请求包成header+body buffer
 * @param in 输入byte buf
 * @return header+body buffer
 * @throws BadSchemaException header格式不对
 * @throws TooBigDataException body太大
 * @throws NotEnoughDataException 可读长度不够，由于粘包拆包问题。
 */
Object decode(DynamicCompositeByteBuf in)
        throws BadSchemaException, TooBigDataException, NotEnoughDataException;
 
/**
 * 客户端序列化请求对象
 * @param rpcRequest 待发送给服务端的对象
 * @throws Exception 序列化异常
 */
ByteBuf encodeRequest(RpcRequest rpcRequest) throws Exception;
 
/**
 * 客户端反序列化rpc响应
 * @param packet header & body的buf
 * @param ctx netty channel context
 * @throws Exception 反序列化异常
 */
RpcResponse decodeResponse(Object packet, ChannelHandlerContext ctx) throws Exception;
 
/**
 * 设置连接被归还入池的时机。
 * 有些协议需要client在连接上保存请求id字段，以便在响应返回时能找到对应的请求，比如http协议、nshead协议就是这样。
 * @return true代表请求发送后立即归还连接，无需等待响应；false等到收到响应后再释放。
 */
boolean returnChannelBeforeResponse();
```

## 新增TCP协议server端实现
```java
/**
 * 客户端/服务端解析请求包成header+body buffer
 * @param in 输入byte buf
 * @return header+body buffer
 * @throws BadSchemaException header格式不对
 * @throws TooBigDataException body太大
 * @throws NotEnoughDataException 可读长度不够，由于粘包拆包问题。
 */
Object decode(DynamicCompositeByteBuf in)
        throws BadSchemaException, TooBigDataException, NotEnoughDataException;
 
 
/**
 * 服务端反序列化rpc请求
 * @param packet header & body的buf
 * @return 输出对象
 */
void decodeRequest(Object packet, RpcRequest rpcRequest) throws Exception;
 
/**
 * 服务端序列化返回结果。
 * @param rpcResponse 服务端要返回给客户端的对象
 * @throws Exception 序列化异常
 */
ByteBuf encodeResponse(RpcResponse rpcResponse) throws Exception;
```

## 新增HTTP协议client端实现
```java
/**
 * 根据rpc request构建http request
 * @param rpcRequest
 * @return
 * @throws Exception
 */
FullHttpRequest encodeHttpRequest(RpcRequest rpcRequest) throws Exception;
 
/**
 * 根据http response构建rpc response
 * @param httpResponse
 * @return
 */
RpcResponse decodeHttpResponse(FullHttpResponse httpResponse, ChannelHandlerContext ctx);
```

## 新增HTTP协议server端实现
```java
/**
 * 根据http request生成rpc request
 * @param httpRequest
 * @return
 */
void decodeHttpRequest(FullHttpRequest httpRequest, RpcRequest rpcRequest);
 
/**
 * 根据rpc response生成http response
 * @param rpcResponse
 * @return
 */
FullHttpResponse encodeHttpResponse(RpcRequest rpcRequest, RpcResponse rpcResponse);
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