package com.baidu.cloud.starlight.benchmark;

import com.baidu.cloud.starlight.benchmark.model.User;
import com.baidu.cloud.starlight.benchmark.service.AsyncUserService;
import com.baidu.cloud.starlight.benchmark.service.UserService;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightClient;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.proxy.JDKProxyFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by liuruisen on 2020/3/11.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 8)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@CompilerControl(CompilerControl.Mode.INLINE)
public class StarlightBenchmark {
    private static AtomicInteger failNum = new AtomicInteger(0);
    // 分四种消息大小分别测试，5byte 1k 2k 4k
    @Param({"/message.txt", "/message_1k.txt", "/message_2k.txt", "/message_4k.txt"})
    private String messageTxt;

    private UserService userService;

    private AsyncUserService asyncUserService;

    private StarlightClient starlightClient;

    private User user;

    public StarlightBenchmark() {

    }

    @Setup
    public void setup() {
        System.out.println("---Setup---" + Thread.currentThread().getName());
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setIoThreadNum(8);
        transportConfig.setWriteTimeoutMills(3000);
        transportConfig.setReadIdleTimeout(3);
        transportConfig.setConnectTimeoutMills(3000);
        transportConfig.setReadIdleTimeout(100);
        transportConfig.setMaxHeartbeatTimes(3);
        transportConfig.setChannelType("pool");

        starlightClient =
                new DefaultStarlightClient("localhost", 8005, transportConfig);
        starlightClient.init();

        // refer
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setProtocol("brpc");
        serviceConfig.setInvokeTimeoutMills(5000);
        // starlightClient.refer(UserService.class, serviceConfig);

        JDKProxyFactory proxyFactory = new JDKProxyFactory();
        userService = proxyFactory.getProxy(UserService.class, serviceConfig, starlightClient);

        asyncUserService = proxyFactory.getProxy(AsyncUserService.class, serviceConfig, starlightClient);

        String message = readBytesFromFile(messageTxt);
        user = new User();
        user.setUserId(123);
        user.setUserName(message);
    }

    @TearDown
    public void tearDown() {
        System.out.println("---TearDown---" + Thread.currentThread().getName());
        starlightClient.destroy();
        System.out.println("---FailNum----" + failNum.get());
    }

    @Benchmark
    public void echo() {
        try {
            userService.updateUser(user);
        } catch (Exception e) {
            failNum.getAndAdd(1);
            System.out.println("Excepetion happen: " + e.getMessage());
        }
    }

    /*@Benchmark
    public void echoAsync() {
        try {
            asyncUserService.updateUserCallback(user, new Callback() {
                @Override
                public void onResponse(Object response) {
                    // System.out.println(response);
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            failNum.getAndAdd(1);
            System.out.println("Excepetion happen: " + e.getMessage());
        }
    }*/


    public static void main(String[] args) {
        Options opt = new OptionsBuilder()
                .include(StarlightBenchmark.class.getSimpleName())
                .threads(32)
                .forks(1)
                .build();
        try {
            new Runner(opt).run();
        } catch (RunnerException e) {
            System.exit(-1);
        }
    }

    public static String readBytesFromFile(String messageTxt) {
        try {
            System.out.println("messageTxt --- " + messageTxt);
            InputStream inputStream = Thread.currentThread().getClass()
                    .getResourceAsStream(messageTxt);
            int messageLength = inputStream.available();
            byte[] messageBytes = new byte[messageLength];
            inputStream.read(messageBytes);
            return new String(messageBytes);
        } catch (IOException ex) {
            System.exit(-1);
        }
        return null;
    }
}
