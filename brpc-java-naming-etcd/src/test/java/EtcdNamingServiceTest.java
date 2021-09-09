import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.naming.etcd.EtcdNamingService;
import com.baidu.brpc.protocol.SubscribeInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EtcdNamingServiceTest {

    private static BrpcURL namingUrl;
    private static EtcdNamingService namingService;

    @BeforeClass
    public static void init() {
        namingUrl = new BrpcURL("etcd://127.0.0.1:2379");
        namingService = new EtcdNamingService(namingUrl);
    }

    protected RegisterInfo createRegisterInfo(String host, int port) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setHost(host);
        registerInfo.setPort(port);
        registerInfo.setInterfaceName(EchoService.class.getName());
        return registerInfo;
    }

    protected SubscribeInfo createSubscribeInfo(boolean ignoreFail) {
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setInterfaceName(EchoService.class.getName());
        subscribeInfo.setIgnoreFailOfNamingService(ignoreFail);
        return subscribeInfo;
    }

    @Test
    public void testLookup(){
        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8079);
        namingService.register(registerInfo);
        SubscribeInfo subscribeInfo = createSubscribeInfo(true);
        List<ServiceInstance>  instances = namingService.lookup(subscribeInfo);
        Assert.assertTrue(instances.size() == 1);
        Assert.assertTrue(instances.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(instances.get(0).getPort() == 8079);
    }

    @Test
    public void testSubscribe() throws Exception{
        final List<ServiceInstance> adds = new ArrayList<ServiceInstance>();
        final List<ServiceInstance> deletes = new ArrayList<ServiceInstance>();
        SubscribeInfo subscribeInfo = createSubscribeInfo(false);
        namingService.subscribe(subscribeInfo, (addList, deleteList) -> {
            System.out.println("receive new subscribe info time:" + System.currentTimeMillis());
            System.out.println("add size:" + addList.size());
            for (ServiceInstance instance : addList) {
                System.out.println(instance);
            }
            adds.addAll(addList);

            System.out.println("delete size:" + deleteList.size());
            for (ServiceInstance instance : deleteList) {
                System.out.println(instance);
            }
            deletes.addAll(deleteList);
        });
        RegisterInfo registerInfo = createRegisterInfo("127.0.0.1", 8079);
        namingService.register(registerInfo);
        System.out.println("register time=" + System.currentTimeMillis());
        Thread.sleep(10*1000);
        Assert.assertTrue(adds.size() == 1);
        Assert.assertTrue(deletes.size() == 0);
        Assert.assertTrue(adds.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(adds.get(0).getPort() == 8079);
        adds.clear();
        deletes.clear();

        namingService.unregister(registerInfo);
        System.out.println("unregister time=" + System.currentTimeMillis());
        Thread.sleep(1000);
        Assert.assertTrue(adds.size() == 0);
        Assert.assertTrue(deletes.size() == 1);
        Assert.assertTrue(deletes.get(0).getIp().equals("127.0.0.1"));
        Assert.assertTrue(deletes.get(0).getPort() == 8079);

        namingService.unsubscribe(subscribeInfo);
    }
}
