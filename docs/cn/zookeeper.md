# Zookeeper注册中心
## Server端注册到Zookeeper用法：
```java
RpcServerOptions options = new RpcServerOptions();
options.setNamingServiceUrl("zookeeper://127.0.0.1:2181");
RpcServer rpcServer = new RpcServer(port, options, new ZookeeperNamingFactory());
```

## Client端注册到Zookeeper用法：
```java
String serviceUrl = "zookeeper://127.0.0.1:2181";
RpcClientOptions clientOption = new RpcClientOptions();
List<Interceptor> interceptors = new ArrayList<Interceptor>();
RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors, new ZookeeperNamingFactory());
```

## Spring环境下的Zookeeper注册中心用法：
```xml
<bean
   class="com.baidu.brpc.spring.annotation.CommonAnnotationBeanPostProcessor">
   <property name="callback">
      <bean class="com.baidu.brpc.spring.annotation.RpcAnnotationResolver">
         <property name="namingServiceUrl" value="zookeeper://127.0.0.1:2181" />
         <property name="namingServiceFactory">
            <bean class="com.baidu.brpc.naming.zookeeper.ZookeeperNamingFactory" />
         </property>
      </bean>
   </property>
</bean>
```