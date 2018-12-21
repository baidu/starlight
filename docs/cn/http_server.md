## 示例程序

[brpc-java-examples/src/main/java/com/baidu/brpc/example/http](https://github.com/baidu/brpc-java/tree/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/http)

## 支持的content-type类型
* application/json
* application/proto

## 定义application json/proto接口类

```java
public interface HelloWorldService {
    @BrpcMeta(serviceName = "HelloWorldService", methodName = "hello")
    String hello(String request);
}
```
* 使用application/json或者application/proto时，uri是serviceName + "/" + methodName，跟baidu-rpc c++版本一致。
* 当BrpcMeta不设置时，框架使用接口全名作为serviceName，方法名作为methodName。
* 当body是proto时，也支持使用jprotobuf方式定义的普通java类作为request/response。

