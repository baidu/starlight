#!/usr/bin/env bash
cd `dirname $0`
PROJECT_DIR=`pwd`/../../../
cd ${PROJECT_DIR}

if [[ $# != 2 ]]; then
    printf "Usage:\n"
    printf "\tsh run_client.sh <server-address> <thread-num>\n"
    printf "Sample:\n"
    printf "\tsh run_client.sh list://127.0.0.1:8002 8\n"
    exit 1
fi

ADDRESS=$1
THREAD_NUM=$2

LOG_DIR="./logs"
if [[ ! -d ${LOG_DIR} ]];then
    mkdir ${LOG_DIR}
fi

JVM_OPTIONS=" -server -Xmn2g -Xmx6g -Xms6g -Xss256k -Xverify:none \
 -XX:+DisableExplicitGC -XX:+AlwaysPreTouch \
 -XX:+AggressiveOpts -XX:AutoBoxCacheMax=20000 \
 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly \
 -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:ParallelGCThreads=4 \
 -XX:+PrintGCDetails  -XX:+PrintGCTimeStamps -Xloggc:./logs/client_gc.log "

CUSTOM_CLASSPATH="target/classes:target/test-classes:target/dependency/* "

MAIN_CLASS="com.baidu.brpc.example.push.BenchmarkClientPushTest"

java ${JVM_OPTIONS} -cp ${CUSTOM_CLASSPATH} ${MAIN_CLASS} ${ADDRESS} ${THREAD_NUM}
