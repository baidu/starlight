package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.thirdparty.netty.util.HashedWheelTimer;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.Ordered;

import java.util.List;
import java.util.Map;

/**
 * Ribbon service list filter defined by Starlight.
 * Support filtering server list peer request, pay attention to the time-consuming execution of filters.
 * Execute in ascending order of {@link Ordered} value.
 *
 * Created by liuruisen on 2021/4/22.
 */
public interface StarlightServerListFilter extends Ordered {

    Logger LOGGER = LoggerFactory.getLogger(StarlightServerListFilter.class);

    /**
     * Timer used to execute recover tasks, such as clean task
     */
    Timer SERVER_LIST_FILTER_TIMER =
            new HashedWheelTimer(new NamedThreadFactory("ServerListRecoverTimer"));

    /**
     * Filter origin server list to get more stable server list.
     * @param originList
     * @return
     */
    List<ServiceInstance> getFilteredList(List<ServiceInstance> originList);

    /**
     * Server list filter timer tasks, such as clean up tasks or recover tasks
     * @return
     */
    Map<String, Timeout> getServerListFilterTasks();

    /**
     * Submit the server list filter timer task
     * @param server
     * @param execDelay
     */
    void submitTimerTask(ServiceInstance server, Integer execDelay);

    /**
     * Destroy
     */
    void destroy();

    /**
     * FIXME 改造为ClientManagerAware的形式
     * @return
     */
    SingleStarlightClientManager getSingleClientManager();
}
