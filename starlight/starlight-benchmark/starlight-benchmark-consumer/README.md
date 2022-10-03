## JMH benchmark性能测试步骤
### IDEA中测试
- 启动starlight-benchmark-provider，BenchmarkProviderApplication类
- 启动starlight-benchmark-consumer中的JMH类，EchoServiceBenchmark类
- 等待测试完成，结果将展示在控制台日志最后
> 将会fork出2个进程进行测试，每个进程有n个测试线程，n的大小与cpu核数一致

### Jar包启动
- 执行mvn package命令打包starlight-benchmark-provider与starlight-benchmark-consumer
- starlight-benchmark-consumer打包后将生成benchmarks.jar与starlight-benchmark-consumer-xx.jar, 
使用benchmarks.jar做基准测试(性能测试)
- 启动starlight-benchmark-provider用于提供rpc服务，启动脚本为sh runBenchmarkProvider.sh
- 运行benchmarks.jar进行基准测试，运行脚本sh runJmhBenchmark.sh [forkNum] [threadNum] [benchmark class name]
- 等待测试完成，结果将展示在jmh-result.csv中
> sh runJmhBenchmark.sh [forkNum] [threadNum] [benchmark class name] 后有3个可选参数，
> 第一个参数含义为测试进程数，第二个参数含义为测试进程的测试线程数,第三个参数含义为测试的benchmark类
> [forkNum] 不填写默认为2，[threadNum] 不填写默认为机器cpu核数 [benchmark class name] benchmark class,不填写默认为EchoServiceBenchmark