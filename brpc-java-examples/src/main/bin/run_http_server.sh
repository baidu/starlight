#!/usr/bin/env bash
cd `dirname $0`
PROJECT_DIR=`pwd`/../../../
cd ${PROJECT_DIR}

LOG_DIR="./logs"
if [[ ! -d ${LOG_DIR} ]];then
    mkdir ${LOG_DIR}
fi

JVM_OPTIONS=" -server -Xmn2g -Xmx6g -Xms6g -Xss256k -Xverify:none \
 -XX:+DisableExplicitGC -XX:+AlwaysPreTouch \
 -XX:+AggressiveOpts -XX:AutoBoxCacheMax=20000 \
 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly \
 -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:ParallelGCThreads=4 \
 -XX:+PrintGCDetails  -XX:+PrintGCTimeStamps -Xloggc:./logs/http_server_gc.log "

CUSTOM_CLASSPATH="target/classes:target/test-classes:target/dependency/* "

MAIN_CLASS="com.baidu.brpc.example.http.proto.RpcServerTest"

java ${JVM_OPTIONS} -cp ${CUSTOM_CLASSPATH} ${MAIN_CLASS}
