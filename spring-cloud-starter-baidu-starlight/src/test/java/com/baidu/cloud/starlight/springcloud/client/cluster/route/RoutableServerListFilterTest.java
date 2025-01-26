package com.baidu.cloud.starlight.springcloud.client.cluster.route;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.xds.GravityXdsClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import org.junit.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.REQUEST_ROUTE_KEY;
import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/11/3.
 */
public class RoutableServerListFilterTest {

    @Test
    public void getFilteredList() {
        RoutableServerListFilter router =
                new RoutableServerListFilter(SingleStarlightClientManager.getInstance(),
                        new StarlightRouteProperties(), "test-app");

        ClusterSelector clusterSelector = new GravityXdsClusterSelector();
        clusterSelector.setServiceId("test-app");
        clusterSelector.setClusterName("outbound|test-app.cpdinf.onlinenew.cluster");
        Map<String, String> meta = new HashMap<>();
        meta.put(Constants.EM_PRODUCT_LINE, "cpdinf");
        meta.put(Constants.EM_PLATFORM, "onlinenew");
        clusterSelector.setMeta(meta);
        RpcContext.getContext().set(REQUEST_ROUTE_KEY, clusterSelector);

        long startTime0 = System.currentTimeMillis();
        List<ServiceInstance> result0 = router.getFilteredList(getServiceInstanceList(1000));
        System.out.println("Route list size " + 1000 + " cost " + (System.currentTimeMillis() - startTime0));
        System.out.println("Route result size " + result0.size());
        assertEquals(1000 / 2, result0.size());


        long startTime = System.currentTimeMillis();
        List<ServiceInstance> result = router.getFilteredList(getServiceInstanceList(5000));
        System.out.println("Route list size" + 5000 + " cost " + (System.currentTimeMillis() - startTime));
        System.out.println("Route result size " + result.size());
        assertEquals(5000 / 2, result.size());


        long startTime2 = System.currentTimeMillis();
        List<ServiceInstance> result2 = router.getFilteredList(getServiceInstanceList(10000));
        System.out.println("Route list size " + 10000 + " cost " + (System.currentTimeMillis() - startTime2));
        System.out.println("Route result size " + result2.size());
        assertEquals(10000 / 2, result2.size());

        long startTime3 = System.currentTimeMillis();
        List<ServiceInstance> result3 = router.getFilteredList(getServiceInstanceList(500));
        System.out.println("Route list size " + 500 + " cost " + (System.currentTimeMillis() - startTime3));
        System.out.println("Route result size " + result3.size());
        assertEquals(500 / 2, result3.size());

    }

    @Test
    public void getFilteredListInstanceMatch() {
        RoutableServerListFilter router =
                new RoutableServerListFilter(SingleStarlightClientManager.getInstance(),
                        new StarlightRouteProperties(), "test-app");

        ClusterSelector clusterSelector = new GravityXdsClusterSelector();
        clusterSelector.setServiceId("test-app");
        clusterSelector.setClusterName("outbound|test-app.cpdinf.onlinenew.cluster");
        Map<String, String> meta = new HashMap<>();
        meta.put(Constants.EM_PRODUCT_LINE, "cpdinf");
        meta.put(Constants.EM_PLATFORM, "online");
        meta.put(Constants.EM_INSTANCE_ID, "1-online-yq");
        clusterSelector.setMeta(meta);
        RpcContext.getContext().set(REQUEST_ROUTE_KEY, clusterSelector);

        long startTime0 = System.currentTimeMillis();
        List<ServiceInstance> result0 = router.getFilteredList(getServiceInstanceList(10));
        System.out.println("Route list size " + 10 + " cost " + (System.currentTimeMillis() - startTime0));
        System.out.println("Route result size " + result0.size());
        assertEquals(1, result0.size());
    }

    @Test
    public void getFilteredListEmpty() {
        RoutableServerListFilter router =
                new RoutableServerListFilter(SingleStarlightClientManager.getInstance(),
                        new StarlightRouteProperties(), "test-app");

        ClusterSelector clusterSelector = new GravityXdsClusterSelector();
        clusterSelector.setServiceId("test-app");
        clusterSelector.setClusterName("outbound|test-app.cpdinf.onlinenew.cluster");
        Map<String, String> meta = new HashMap<>();
        meta.put(Constants.EM_PRODUCT_LINE, "cpdinf");
        meta.put(Constants.EM_PLATFORM, "online");
        meta.put(Constants.EM_INSTANCE_ID, "1-online-yq");
        clusterSelector.setMeta(meta);
        RpcContext.getContext().set(REQUEST_ROUTE_KEY, clusterSelector);

        List<ServiceInstance> result0 = router.getFilteredList(new ArrayList<>());
        assertEquals(0, result0.size());
    }

    @Test
    public void getFilterListOrigin() {
        RoutableServerListFilter router =
                new RoutableServerListFilter(SingleStarlightClientManager.getInstance(),
                        new StarlightRouteProperties(), "test-app");

        List<ServiceInstance> result0 = router.getFilteredList(getServiceInstanceList(10));
        assertEquals(10, result0.size());
    }

    private List<ServiceInstance> getServiceInstanceList(int size) {
        List<ServiceInstance> servers = new ArrayList<>();
        for (int i = 0; i < size; i++) {

            Map<String, String> labels = new HashMap<>();
            labels.put("EM_PHY_IDC", "yq01");
            labels.put("EM_HOST_NAME", "localhost");
            labels.put("EM_INSTANCE_ID", i + ".test-test-test-000-test.test.test");
            labels.put("EM_PLATFORM", "online");
            labels.put("env", "online");
            labels.put("GRAVITY_CLIENT_VERSION", "2020.0.2-SNAPSHOT");
            labels.put("EM_LOGIC_IDC", "yq");
            labels.put("EM_PRODUCT_LINE", "cpdinf");
            labels.put("MATRIX_HOST_IP", "10.102.118.45");
            labels.put("EPOCH", "1619676145691");
            labels.put("EM_ENV_TYPE", "ONLINE");
            labels.put("protocols", "brpc,stargate,springrest");

            if (i % 2 == 0) {
                labels.put("EM_PLATFORM", "onlinenew");
            }

            DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
            serviceInstance.setServiceId("test-app");
            serviceInstance.setInstanceId("test-id-" + i);
            serviceInstance.setPort(i);
            serviceInstance.setHost("localhost");

            servers.add(serviceInstance);
        }

        return servers;
    }
}