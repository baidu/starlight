## 示例程序

[brpc-java-examples/src/main/java/com/baidu/brpc/example/nshead](https://github.com/baidu/brpc-java/tree/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/nshead)

## 定义请求和响应类
目前序列化方式使用protobuf，也支持使用jprotobuf方式定义的普通java类作为request/response，
具体使用例子请见[brpc-java-examples/src/main/java/com/baidu/brpc/example/jprotobuf](https://github.com/baidu/brpc-java/tree/master/brpc-java-examples/src/main/java/com/baidu/brpc/example/jprotobuf)。

```proto
package example;
option cc_generic_services = true;
option java_package="com.baidu.brpc.example.standard";
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


## 定义java接口

nshead接口类
```java
public interface EchoService {
    @NSHeadMeta(provider = "coeus-cpc")
    Echo.EchoResponse echo(Echo.EchoRequest request);
}
```

* NSHeadMeta标记在接口方法上，用于设置NSHead相关属性，包括provider、id、version，默认可以不填。
* nshead协议server端只支持一个接口一个方法，因为nshead协议中没有字段来唯一定位一个方法。