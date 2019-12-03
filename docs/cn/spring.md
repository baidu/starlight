# Spring集成支持
## 引入maven依赖
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-spring-boot-starter</artifactId>
    <version>2.5.8</version>
</dependency>
```
## 接口声明跟非Spring用法一样
## 在application.yml中添加配置：
```yaml
brpc:
  global:
    naming:
      namingServiceUrl: zookeeper://127.0.0.1:2181/examples # 注册中心名称
      group: "normal" # 实例分组名称，用于区分同一个接口不同提供方场景，默认default
      version: 1.0.0 # 提供方接口实现版本
      ignoreFailOfNamingService: false # false：注册中心连不上时会报错。
    server: # server配置跟RpcServerOptions一样
      port: 8002
      workThreadNum: 1
      ioThreadNum: 1
    client: # client配置和RpcClientOptions一样
      workThreadNum: 1
      ioThreadNum: 1
  custom:
    com.baidu.brpc.example.springboot.api.EchoService:
      naming:
        version: 2.0.0
    com.baidu.brpc.example.springboot.api.AsyncEchoService:
      naming:
        version: 2.0.0
```
* brpc.global是默认配置，brpc.custom是具体接口的自定义配置，当同一个配置在brpc.custom和brpc.global同时存在时，以brpc.custom为准。
* brpc.global.server只有在使用RpcServer时才需要。
* brpc.global.client只有在使用RpcClient时才需要。

## Server端使用@RpcExporter暴露服务
```java
@RpcExporter
public class EchoServiceImpl implements EchoService {
    @Override
    public EchoResponse echo(EchoRequest request) {
        String message = request.getMessage();
        EchoResponse response = new EchoResponse();
        response.setMessage(message);
        return response;
    }
}
```
## Client端使用@RpcProxy生成接口代理对象
```java
@Service
public class EchoFacadeImpl implements EchoFacade {
    @RpcProxy
    private EchoService echoService;
 
    /**
     * async service interface proxy will create new RpcClient,
     * not used RpcClient of sync interface proxy.
     */
    @RpcProxy
    private AsyncEchoService echoService3;
 
    public EchoResponse echo(EchoRequest request) {
        System.out.println(echoService.hashCode());
        return echoService.echo(request);
    }
 
    public Future<EchoResponse> echo3(EchoRequest request) {
        System.out.println(echoService3.hashCode());
        Future<EchoResponse> future = echoService3.echo(request, new RpcCallback<EchoResponse>() {
            @Override
            public void success(EchoResponse response) {
                System.out.println(response.getMessage());
            }
 
            @Override
            public void fail(Throwable e) {
                e.printStackTrace();
            }
        });
        return future;
    }
}
```
