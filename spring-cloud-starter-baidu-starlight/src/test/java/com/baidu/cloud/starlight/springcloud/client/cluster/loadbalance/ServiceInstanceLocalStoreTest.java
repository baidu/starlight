package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.ServiceInstanceLocalStore;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import org.junit.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/7/27.
 */
public class ServiceInstanceLocalStoreTest {


    @Test
    public void loadLocalCache() {
        File file = new File(getCacheFileName("testApp-load"));
        if (file.exists()) {
            file.delete();
        }

        ServiceInstanceLocalStore localStore =
                new ServiceInstanceLocalStore("testApp-load", new StarlightClientProperties());

        localStore.loadCachedListOfServers();


        Class lbClass = localStore.getClass();

        try {
            Field cacheFileField = lbClass.getDeclaredField("cacheFile");
            cacheFileField.setAccessible(true);

            Field cacheField = lbClass.getDeclaredField("localSrvListCache");
            cacheField.setAccessible(true);

            assertNotNull(cacheFileField.get(localStore));
            assertTrue(((File) cacheFileField.get(localStore)).exists());
            assertNotNull(cacheField.get(localStore));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        localStore.close();
    }

    @Test
    public void updateLocalCache() {
        File file = new File(getCacheFileName("testApp-update"));
        if (file.exists()) {
            file.delete();
        }

        ServiceInstanceLocalStore localStore =
                new ServiceInstanceLocalStore("testApp-update", new StarlightClientProperties());

        try {
            Class lbClass = localStore.getClass();
            Field cacheField = lbClass.getDeclaredField("localSrvListCache");
            cacheField.setAccessible(true);
            Properties localCache = (Properties) cacheField.get(localStore);
            assertEquals(0, localCache.entrySet().size());


            List<ServiceInstance> servers = getServiceInstanceList(50000);

            localStore.updateCachedListOfServers(servers);

            Properties localCacheAfter = (Properties) cacheField.get(localStore);
            assertEquals(1, localCacheAfter.entrySet().size());

            localStore.close();

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getLocalCache() {
        File file = new File(getCacheFileName("testApp-get"));
        if (file.exists()) {
            file.delete();
        }

        ServiceInstanceLocalStore localStore =
                new ServiceInstanceLocalStore("testApp-get", new StarlightClientProperties());

        List serverList = localStore.getCachedListOfServers();
        assertEquals(0, serverList.size());
        List<ServiceInstance> servers = getServiceInstanceList(50000);
        localStore.updateCachedListOfServers(servers);

        List serverList2 = localStore.getCachedListOfServers();
        assertEquals(50000, serverList2.size());

        localStore.close();
    }

    @Test
    public void createCacheFile() {
        ServiceInstanceLocalStore localStore =
                new ServiceInstanceLocalStore("testApp-get", new StarlightClientProperties());

        String filePath = "starlight/test/cache/cache.txt";
        localStore.createCacheFile(filePath);

        assertTrue(new File(filePath).getParentFile().exists());
        assertTrue(new File(filePath).exists());

        String filePath2 = "starlight/test/cache/cache2.txt";
        localStore.createCacheFile(filePath2);
        assertTrue(new File(filePath2).getParentFile().exists());
        assertTrue(new File(filePath2).exists());


    }

    private String getCacheFileName(String clientName) {
        return System.getProperty("java.io.tmpdir") + "/starlight/local-registry/" + clientName + ".cache";
    }

    private List<ServiceInstance> getServiceInstanceList(int size) {
        List<ServiceInstance> servers = new ArrayList<>();
        for (int i = 0; i < size; i++) {

            Map<String, String> labels = new HashMap<>();
            labels.put("EM_PHY_IDC", "yq01");
            labels.put("EM_HOST_NAME", "yq01-ecom-fengchao-emsin7.yq01.baidu.com");
            labels.put("EM_INSTANCE_ID", "2.opera-online-starlightProvider-000-yq.FENGCHAO.yq01");
            labels.put("EM_PLATFORM", "online");
            labels.put("env", "online");
            labels.put("GRAVITY_CLIENT_VERSION", "2020.0.2-SNAPSHOT");
            labels.put("EM_LOGIC_IDC", "yq");
            labels.put("EM_PRODUCT_LINE", "cpdinf");
            labels.put("MATRIX_HOST_IP", "10.102.118.45");
            labels.put("EPOCH", "1619676145691");
            labels.put("EM_ENV_TYPE", "ONLINE");
            labels.put("protocols", "brpc,stargate,springrest");


            DefaultServiceInstance serviceInstance = new DefaultServiceInstance("test-id-" + i, "test-app",
                    "localhost", i, true, labels);

            servers.add(serviceInstance);
        }

        return servers;
    }

}