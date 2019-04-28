package com.baidu.brpc.naming.zookeeper;

import com.baidu.brpc.client.instance.Endpoint;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.naming.SubscribeInfo;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ZookeeperNamingServiceTest {
    private TestingServer zkServer;
    private BrpcURL namingUrl;
    private ZookeeperNamingService namingService;

    public void setUp() throws Exception {
        zkServer = new TestingServer(2087, true);
        namingUrl = new BrpcURL("zookeeper://127.0.0.1:2087");
        namingService = new ZookeeperNamingService(namingUrl);
    }

    public void tearDown() throws Exception {
        zkServer.stop();
    }

    protected RegisterInfo createRegisterInfo(String host, int port) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setHost(host);
        registerInfo.setPort(port);
        registerInfo.setService(EchoService.class.getName());
        return registerInfo;
    }

    protected SubscribeInfo createSubscribeInfo(boolean ignoreFail) {
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setService(EchoService.class.getName());
        subscribeInfo.setIgnoreFailOfNamingService(ignoreFail);
        return subscribeInfo;
    }

    @Test
    public void testLookup() throws Exception {
        setUp();
        SubscribeInfo subscribeInfo = createSubscribeInfo(true);
        List<Endpoint> endPoints = namingService.lookup(subscribeInfo);
        Assert.assertTrue(endPoints.size() == 0);

        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8012);
        namingService.register(registerInfo);
        endPoints = namingService.lookup(subscribeInfo);
        Assert.assertTrue(endPoints.size() == 1);
        Assert.assertTrue(endPoints.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(endPoints.get(0).getPort() == 8012);
        namingService.unregister(registerInfo);
        tearDown();
    }

    @Test
    public void testSubscribe() throws Exception {
        setUp();
        final List<Endpoint> adds = new ArrayList<Endpoint>();
        final List<Endpoint> deletes = new ArrayList<Endpoint>();
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        namingService.subscribe(subscribeInfo, new NotifyListener() {
            @Override
            public void notify(Collection<Endpoint> addList, Collection<Endpoint> deleteList) {
                System.out.println("receive new subscribe info time:" + System.currentTimeMillis());
                System.out.println("add size:" + addList.size());
                for (Endpoint endPoint : addList) {
                    System.out.println(endPoint);
                }
                adds.addAll(addList);

                System.out.println("delete size:" + deleteList.size());
                for (Endpoint endPoint : deleteList) {
                    System.out.println(endPoint);
                }
                deletes.addAll(deleteList);
            }
        });
        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8013);
        namingService.register(registerInfo);
        System.out.println("register time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 1);
        Assert.assertTrue(deletes.size() == 0);
        Assert.assertTrue(adds.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(adds.get(0).getPort() == 8013);
        adds.clear();
        deletes.clear();

        namingService.unregister(registerInfo);
        System.out.println("unregister time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 0);
        Assert.assertTrue(deletes.size() == 1);
        Assert.assertTrue(deletes.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(deletes.get(0).getPort() == 8013);

        namingService.unsubscribe(subscribeInfo);
        tearDown();
    }

    /**
     * This test must test under actual zookeeper server, Not the TestingServer of Curator
     */
    @Test
    @Ignore
    public void testSubscribeWhenZookeeperDownAndUp() throws Exception {
        namingUrl = new BrpcURL("zookeeper://127.0.0.1:2181");
        namingService = new ZookeeperNamingService(namingUrl);

        final List<Endpoint> adds = new ArrayList<Endpoint>();
        final List<Endpoint> deletes = new ArrayList<Endpoint>();
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        namingService.subscribe(subscribeInfo, new NotifyListener() {
            @Override
            public void notify(Collection<Endpoint> addList, Collection<Endpoint> deleteList) {
                System.out.println("receive new subscribe info time:" + System.currentTimeMillis());
                System.out.println("add size:" + addList.size());
                for (Endpoint endPoint : addList) {
                    System.out.println(endPoint);
                }
                adds.addAll(addList);

                System.out.println("delete size:" + deleteList.size());
                for (Endpoint endPoint : deleteList) {
                    System.out.println(endPoint);
                }
                deletes.addAll(deleteList);
            }
        });
        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8014);
        namingService.register(registerInfo);
        System.out.println("register time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 1);
        Assert.assertTrue(deletes.size() == 0);
        Assert.assertTrue(adds.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(adds.get(0).getPort() == 8014);
        adds.clear();
        deletes.clear();

        // sleep for restarting zookeeper
        Thread.sleep(30 * 1000);

        List<Endpoint> endPoints = namingService.lookup(subscribeInfo);
        Assert.assertTrue(endPoints.size() == 1);
        Assert.assertTrue(endPoints.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(endPoints.get(0).getPort() == 8014);

        namingService.unregister(registerInfo);
        System.out.println("unregister time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 0);
        Assert.assertTrue(deletes.size() == 1);
        Assert.assertTrue(deletes.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(deletes.get(0).getPort() == 8014);

        namingService.unsubscribe(subscribeInfo);
    }

    /**
     * This test must test under actual zookeeper server, Not the TestingServer of Curator
     */
    @Test
    @Ignore
    public void testRegisterWhenZookeeperDownAndUp() throws Exception {
        namingUrl = new BrpcURL("zookeeper://127.0.0.1:2181");
        namingService = new ZookeeperNamingService(namingUrl);

        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8015);
        namingService.register(registerInfo);
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        List<Endpoint> endPoints = namingService.lookup(subscribeInfo);
        Assert.assertTrue(endPoints.size() == 1);
        Assert.assertTrue(endPoints.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(endPoints.get(0).getPort() == 8015);

        // sleep for restarting zookeeper
        Thread.sleep(30 * 1000);
        endPoints = namingService.lookup(subscribeInfo);
        Assert.assertTrue(endPoints.size() == 1);
        System.out.println(endPoints.get(0));
        Assert.assertTrue(endPoints.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(endPoints.get(0).getPort() == 8015);
        namingService.unregister(registerInfo);
    }
}
