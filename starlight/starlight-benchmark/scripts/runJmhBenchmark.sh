#!/bin/bash
echo "Start running JmhBenchmark"

if test $# -eq 3 # $1=Processor num $2=Thread num $3=Benchmark Class Name
then
nohup java -Djmh.ignoreLock=true -jar benchmarks.jar -f $1 -t $2 $3 -rf csv -rff jmh-result.csv >benchmarks.log 2>&1 &
elif test $# -eq 2 # $1=Processor num $2=Thread num
then
    nohup java -Djmh.ignoreLock=true -jar benchmarks.jar -f $1 -t $2 EchoServiceBenchmark -rf csv -rff jmh-result.csv >benchmarks.log 2>&1 &
else
    nohup java -Djmh.ignoreLock=true -jar benchmarks.jar EchoServiceBenchmark -rf csv -rff jmh-result.csv >benchmarks.log 2>&1 &
fi