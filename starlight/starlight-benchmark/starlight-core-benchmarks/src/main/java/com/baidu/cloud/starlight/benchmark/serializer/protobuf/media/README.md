## media包介绍
为了让使用jmh进行的protobuf序列化框架benchmark结果具有可解释与可信性，
借用github上2.8k star的开源项目[jvm-serializers](https://github.com/eishay/jvm-serializers)做为参照。

与其项目中测试条件保持一致中，主要包括
- 测试数据一致：数据结构、数据类型、数据内容均一致
- 序列化框架使用方式一致：序列化/反序列化数据时使用的框架API一致
- 序列化框架jar包版本一致：protobuf-java 3.6.1 protostuff-core/runtime 1.5.9
- 数据采集范围一致：序列化方法内容为对象设值+序列化，反序列化方法内容为反序列化

因只涉及protobuf格式的序列化和反序列化，参照的为jvm-serializers的protobuf/protostuff, protobuf/protostuff-runtime,
protobuf。
- protobuf/protostuff ： 使用protostuff框架进行protobuf格式的序列化和反序列化，手写Schema
- protobuf/protostuff-runtime ： 使用protostuff框架进行protobuf格式的序列化和反序列化， 运行时反射生成Schema
- protobuf： 使用protobuf-java框架进行序列化和反序列化