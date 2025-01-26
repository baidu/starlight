package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.lalb;

import com.baidu.cloud.starlight.api.statistics.Stats;
import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LALB loadbalancer
 * 耗时目标暂定 20ms内
 * TODO unit test
 * Created by liuruisen on 2020/10/21.
 */
public class LocalityAwareLoadBalancer extends RoundRobinLoadBalancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalityAwareLoadBalancer.class);

    public static final String LALB_STATS_KEY = "lalb_latency_stats";

    /**
     * default weight value
     */
    private static final long DEFAULT_WEIGHT = 30;

    /**
     * least instance count
     */
    private static final int MIN_INSTANCE_NUM = 3;

    private static final float ACTIVE_INSTANCE_RATIO = 0.7f;

    private static final int MIN_LATENCY_WINDOW = 3;

    private WeightTreeNode<ServiceInstance> weightTree;

    /**
     * Estimate suitable weight when calculate weight <= 0
     *
     * @see #serviceInstanceWeight(LalbLatencyStats, Long)
     */
    private AtomicLong maxWeight = new AtomicLong(0L);

    private ObjectProvider<ServiceInstanceListSupplier> instanceListSupplierObjectProvider;

    public LocalityAwareLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> instancesListSupplierProvider,
                                     String name) {
        super(instancesListSupplierProvider, name);
        this.weightTree = new WeightTreeNode<>();
        this.instanceListSupplierObjectProvider = instancesListSupplierProvider;
    }


    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 经测试1000台实例耗时可维持在10ms内，先不进行异步处理
        updateWeightTree(request);
        // 第一次请求 weight为0
        if (weightTree.getWeight() == 0) {
            Mono<Response<ServiceInstance>> monResponse = super.choose(request);
            Response<ServiceInstance> loadBalancerResponse = monResponse.block();
            if (loadBalancerResponse != null) {
                addLalbLatencyStats(loadBalancerResponse.getServer());
            }
            return monResponse;
        }

        long totalWeight = weightTree.getWeight();
        long randomWeight = ThreadLocalRandom.current().nextLong(totalWeight);
        WeightTreeNode<ServiceInstance> choseNode = searchNode(weightTree, randomWeight);
        ServiceInstance result = choseNode.getNodeEntity();
        addLalbLatencyStats(result);
        return Mono.just(new DefaultResponse(result));
    }

    protected <T> WeightTreeNode<T> searchNode(WeightTreeNode<T> weightTree, long weight) {

        if (weightTree.getLeftNode() == null) {
            return weightTree;
        }

        if (weightTree.getRightNode() == null) {
            return weightTree.getLeftNode();
        }

        if (weightTree.getLeftNode().getWeight() >= weight) {
            return searchNode(weightTree.getLeftNode(), weight);
        } else {
            return searchNode(weightTree.getRightNode(), weight - weightTree.getLeftNode().getWeight());
        }

    }


    // 完全二叉权重树的构建
    protected synchronized void updateWeightTree(Request request) {
        try {
            List<ServiceInstance> servers = serviceInstances(request);
            if (servers != null && servers.size() > 0) {
                // calculate
                this.weightTree = generateWeightTreeByNodes(weightTreeNodes(servers));
            } else {
                this.weightTree = new WeightTreeNode<>();
            }
        } catch (Exception e) {
            LOGGER.error("Update ServiceInstance weight tree error", e);
        }
    }

    /**
     * 生成权重树节点
     *
     * @param serviceInstances
     * @return
     */
    protected Queue<WeightTreeNode<ServiceInstance>> weightTreeNodes(List<ServiceInstance> serviceInstances) {
        Queue<WeightTreeNode<ServiceInstance>> weightTreeNodes = new LinkedList<>();

        long startTime = System.currentTimeMillis();

        if (serviceInstances != null) {
            Map<LalbLatencyStats, ServiceInstance> lalbLatencyStatsMap = new HashMap<>();
            List<Long> avgLatencies = new LinkedList<>();

            // get all serviceInstanceStats, include requested and unrequested instances
            for (ServiceInstance serviceInstance : serviceInstances) {
                // 没有即创建，没有表示尚未调用到的实例[未调用到、新增实例]
                StarlightStatistics statistics =
                        StarlightStatsManager.getOrCreateStatsByHostPort(InstanceUtils.ipPortStr(serviceInstance));

                Stats stats = statistics.discoverStats(LALB_STATS_KEY);
                if (!(stats instanceof LalbLatencyStats)) {
                    stats = new LalbLatencyStats();
                    statistics.registerStats(LALB_STATS_KEY, stats);
                }
                Queue<Long> latencyWindow = ((LalbLatencyStats) stats).getLatencyWindow();
                if (latencyWindow.size() >= MIN_LATENCY_WINDOW) {
                    lalbLatencyStatsMap.put((LalbLatencyStats) stats, serviceInstance);
                    avgLatencies.add(((LalbLatencyStats) stats).avgLatency());
                } else {
                    // default weight
                    weightTreeNodes.add(new WeightTreeNode<>(serviceInstanceHashCode(serviceInstance),
                            DEFAULT_WEIGHT, serviceInstance));
                }
            }

            LOGGER.debug("LocalityAwareRule weightTreeNodes#lalbLatencyStatsMap cost {}ms",
                    System.currentTimeMillis() - startTime);

            // 具有统计信息的server实例数较少，或者未达到一定比重，认为没有统计计算价值
            if (lalbLatencyStatsMap.size() < MIN_INSTANCE_NUM ||
                    lalbLatencyStatsMap.size() * 1.0 / serviceInstances.size() < ACTIVE_INSTANCE_RATIO) {
                Long execCost = System.currentTimeMillis() - startTime;
                LOGGER.debug("LocalityAwareRule weightTreeNodes cost {}ms", execCost);
                if (execCost > 20) {
                    LOGGER.warn("LocalityAwareRule weightTreeNodes cost {}ms", execCost);
                }
                return weightTreeNodes;
            }

            // calculate the avg latency of all service instance as a benchmark
            Long predictedMaxLatency = latency90Percentile(avgLatencies);

            for (Map.Entry<LalbLatencyStats, ServiceInstance> entry : lalbLatencyStatsMap.entrySet()) {
                long weight = serviceInstanceWeight(entry.getKey(), predictedMaxLatency);
                weightTreeNodes.add(
                        new WeightTreeNode<>(serviceInstanceHashCode(entry.getValue()),
                                weight, entry.getValue()));
            }
        }
        Long execCost = System.currentTimeMillis() - startTime;
        LOGGER.debug("LocalityAwareRule weightTreeNodes cost {}ms", execCost);
        if (execCost > 20) {
            LOGGER.warn("LocalityAwareRule weightTreeNodes cost {}ms", execCost);
        }

        return weightTreeNodes;
    }

    /**
     * Calculate the latency90Percentile of all {@link ServiceInstance} below serviceId
     * 90分位值
     *
     * @param avgLatencies
     * @return
     */
    protected Long latency90Percentile(List<Long> avgLatencies) {
        int percentileIndex = Double.valueOf(avgLatencies.size() * 0.9).intValue() - 1;
        if (percentileIndex < 0) {
            percentileIndex = 0;
        }
        Collections.sort(avgLatencies);
        return avgLatencies.get(percentileIndex);
    }


    /**
     * Calculate the serviceInstance's weight.
     * 归一化为100内的权重
     *
     * @param stats
     * @param predictedMaxLatency
     * @return
     */
    protected long serviceInstanceWeight(LalbLatencyStats stats, Long predictedMaxLatency) {
        Long fixedWindowAvgLatency = stats.avgLatency();
        // normalization to 1-100 to prevent inaccurate calculation, plus 10ms to the predictedMaxLatency to prevent 0
        Long normalizedAvgLatency = fixedWindowAvgLatency * 100 / (predictedMaxLatency + 10);

        long serviceInstanceWeight = 100 - normalizedAvgLatency;

        // Avoid instance with a weight of 0 and no chance to access
        long weight = 0;
        if (serviceInstanceWeight > 0) {
            weight =
                    serviceInstanceWeight > (maxWeight.get() / 10) ?
                            serviceInstanceWeight : (maxWeight.get() / 10) + serviceInstanceWeight;
        } else {
            weight = maxWeight.get() > 0 ? (maxWeight.get() / 10) : 1;
        }

        if (weight > maxWeight.get()) {
            maxWeight.getAndSet(weight);
        }

        return weight;
    }

    /**
     * Calculate the hashcode of {@link ServiceInstance}
     *
     * @param serviceInstance
     * @return
     */
    protected int serviceInstanceHashCode(ServiceInstance serviceInstance) {
        return Objects.hash(serviceInstance);
    }


    /**
     * Generate the weight tree
     *
     * @param weightTreeNodes
     * @return
     */
    protected <T> WeightTreeNode<T> generateWeightTreeByNodes(Queue<WeightTreeNode<T>> weightTreeNodes) {
        if (weightTreeNodes == null || weightTreeNodes.size() == 0) {
            return new WeightTreeNode<>();
        }

        long nodeSize = weightTreeNodes.size();

        if (nodeSize == 1) {
            return weightTreeNodes.poll();
        }

        long startTime = System.currentTimeMillis();

        if (nodeSize % 2 != 0) {
            weightTreeNodes.add(new WeightTreeNode<>()); // transform into a complete binary tree
        }

        WeightTreeNode<T> rootNode = new WeightTreeNode<>();

        while (weightTreeNodes.size() > 0) {

            WeightTreeNode<T> leftChildNode = weightTreeNodes.poll();
            WeightTreeNode<T> rightChildNode = weightTreeNodes.poll();

            if (leftChildNode == null) {
                LOGGER.warn("Left node is null, break");
                break;
            }

            if (rightChildNode == null) {
                rootNode = leftChildNode; // root node
                break;
            }

            WeightTreeNode<T> parentNode = new WeightTreeNode<>(0, 0);
            parentNode.setLeftNode(leftChildNode);
            leftChildNode.setParentNode(parentNode);

            parentNode.setRightNode(rightChildNode);
            rightChildNode.setParentNode(parentNode);

            parentNode.setWeight(leftChildNode.getWeight() + rightChildNode.getWeight());
            parentNode.setChildSize(2 + leftChildNode.getChildSize() + rightChildNode.getChildSize());

            weightTreeNodes.add(parentNode);
        }

        long cost = System.currentTimeMillis() - startTime;
        if (cost > 20) {
            LOGGER.warn("LocalityAwareRule generateWeightTreeByNodes cost {}ms", cost);
        }
        LOGGER.debug("LocalityAwareRule generateWeightTreeByNodes cost {}ms", cost);
        return rootNode;
    }

    private void addLalbLatencyStats(ServiceInstance server) {
        StarlightStatistics statistics =
                StarlightStatsManager.getOrCreateStatsByHostPort(InstanceUtils.ipPortStr(server));
        // putIfAbsent
        statistics.registerStats(LALB_STATS_KEY, new LalbLatencyStats());
    }

    /**
     * For test
     * @return
     */
    protected WeightTreeNode<ServiceInstance> getWeightTree() {
        return weightTree;
    }


    private List<ServiceInstance> serviceInstances(Request request) {
        ServiceInstanceListSupplier supplier =
                instanceListSupplierObjectProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);

        return supplier.get(request).next().block();
    }
}
