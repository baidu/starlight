## starlight-benchmark
Starlight的benchmark机制，所有关于性能测试的取舍等均在此编写，以目录(package)作为区分。

使用JMH框架作为基准测试框架，搭配JMH Visualizer进行可视化

### starlight-core-benchmarks
框架内部采用的关键技术点的benchmark，如序列化框架等micro benchmark

### starlight-benchmark-*
使用starlight-core构建的直连通信的客户端调用服务端的性能

## 测试机器
CPU: 

Core: 

Memory: