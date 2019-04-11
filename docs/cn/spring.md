# Spring集成支持
## 引入maven依赖
```xml
<dependency>
    <groupId>com.baidu</groupId>
    <artifactId>brpc-java-spring</artifactId>
    <version>2.0.0</version>
</dependency>
```
## 接口声明跟非Spring用法一样
## xml配置（Client/Server都需要）
```xml
<context:component-scan base-package="com.baidu.brpc.example.spring.server">
<bean class="com.baidu.brpc.spring.annotation.CommonAnnotationBeanPostProcessor">
    <property name="callback">
        <bean class="com.baidu.brpc.spring.annotation.RpcAnnotationResolver" />
    </property>
</bean>
```
## Server端使用@RpcExporter暴露服务
```java
@Component
@RpcExporter(port = "8012")
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
    @RpcProxy(serviceUrl = "list://127.0.0.1:8012",
            rpcClientOptionsBeanName = "rpcClientOptions",
            interceptorBeanName = "customInterceptor")
    private EchoService echoService;
 
    /**
     * async service interface proxy will create new RpcClient,
     * not used RpcClient of sync interface proxy.
     */
    @RpcProxy(serviceUrl = "list://127.0.0.1:8012",
            lookupStubOnStartup = false,
            rpcClientOptionsBeanName = "rpcClientOptions",
            interceptorBeanName = "customInterceptor")
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
