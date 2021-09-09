import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.naming.etcd.EtcdNamingService;
import org.junit.BeforeClass;
import org.junit.Test;

public class EtcdNamingServiceTest {

    private BrpcURL namingUrl;
    private EtcdNamingService namingService;
    @BeforeClass
    public void init(){
        namingUrl = new BrpcURL("etcd://172.20.50.157:2379");
        namingService = new EtcdNamingService(namingUrl);
    }

    protected RegisterInfo createRegisterInfo(String host, int port) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setHost(host);
        registerInfo.setPort(port);
        registerInfo.setInterfaceName(EchoService.class.getName());
        return registerInfo;
    }

    @Test
    public void testDoRegister(){
        namingService.register(createRegisterInfo("127.0.0.1",8019));
    }
}
