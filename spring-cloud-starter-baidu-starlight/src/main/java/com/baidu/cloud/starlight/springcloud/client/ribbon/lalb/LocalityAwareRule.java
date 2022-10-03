/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.springcloud.client.ribbon.lalb;

import com.baidu.cloud.starlight.api.statistics.Stats;
import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;

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
 * LALB loadbalancer 耗时目标暂定 20ms内 Created by liuruisen on 2020/10/21.
 */
public class LocalityAwareRule extends RandomRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalityAwareRule.class);

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

    private WeightTreeNode<Server> weightTree;

    /**
     * Estimate suitable weight when calculate weight <= 0
     *
     * @see #serviceInstanceWeight(LalbLatencyStats, Long)
     */
    private AtomicLong maxWeight = new AtomicLong(0L);

    public LocalityAwareRule() {
        this.weightTree = new WeightTreeNode<>();
    }

    @Override
    public void setLoadBalancer(ILoadBalancer lb) {
        super.setLoadBalancer(lb);
    }

    @Override
    public Server choose(Object key) {
        // 经测试1000台实例耗时可维持在10ms内，先不进行异步处理
        updateWeightTree();
        // 第一次请求 weight为0
        if (weightTree.getWeight() == 0) {
            Server result = super.choose(key);
            addLalbLatencyStats(result);
            return result;
        }

        long totalWeight = weightTree.getWeight();
        long randomWeight = ThreadLocalRandom.current().nextLong(totalWeight);
        WeightTreeNode<Server> choseNode = searchNode(weightTree, randomWeight);
        Server result = choseNode.getNodeEntity();
        addLalbLatencyStats(result);
        return result;
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
    protected synchronized void updateWeightTree() {
        try {
            List<Server> servers = getLoadBalancer().getAllServers();
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
    protected Queue<WeightTreeNode<Server>> weightTreeNodes(List<Server> serviceInstances) {
        Queue<WeightTreeNode<Server>> weightTreeNodes = new LinkedList<>();

        long startTime = System.currentTimeMillis();

        if (serviceInstances != null) {
            Map<LalbLatencyStats, Server> lalbLatencyStatsMap = new HashMap<>();
            List<Long> avgLatencies = new LinkedList<>();

            // get all serviceInstanceStats, include requested and unrequested instances
            for (Server serviceInstance : serviceInstances) {
                // 没有即创建，没有表示尚未调用到的实例[未调用到、新增实例]
                StarlightStatistics statistics =
                    StarlightStatsManager.getOrCreateStatsByHostPort(serviceInstance.getHostPort());

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
                    weightTreeNodes.add(new WeightTreeNode<>(serviceInstanceHashCode(serviceInstance), DEFAULT_WEIGHT,
                        serviceInstance));
                }
            }

            LOGGER.debug("LocalityAwareRule weightTreeNodes#lalbLatencyStatsMap cost {}ms",
                System.currentTimeMillis() - startTime);

            // 具有统计信息的server实例数较少，或者未达到一定比重，认为没有统计计算价值
            if (lalbLatencyStatsMap.size() < MIN_INSTANCE_NUM
                || lalbLatencyStatsMap.size() * 1.0 / serviceInstances.size() < ACTIVE_INSTANCE_RATIO) {
                Long execCost = System.currentTimeMillis() - startTime;
                LOGGER.debug("LocalityAwareRule weightTreeNodes cost {}ms", execCost);
                if (execCost > 20) {
                    LOGGER.warn("LocalityAwareRule weightTreeNodes cost {}ms", execCost);
                }
                return weightTreeNodes;
            }

            // calculate the avg latency of all service instance as a benchmark
            Long predictedMaxLatency = latency90Percentile(avgLatencies);

            for (Map.Entry<LalbLatencyStats, Server> entry : lalbLatencyStatsMap.entrySet()) {
                long weight = serviceInstanceWeight(entry.getKey(), predictedMaxLatency);
                weightTreeNodes
                    .add(new WeightTreeNode<>(serviceInstanceHashCode(entry.getValue()), weight, entry.getValue()));
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
     * Calculate the latency90Percentile of all {@link ServiceInstance} below serviceId 90分位值
     *
     * @param avgLatencies
     * @return
     */
    protected Long latency90Percentile(List<Long> avgLatencies) {
        int percentileIndex = new Double(avgLatencies.size() * 0.9).intValue() - 1;
        if (percentileIndex < 0) {
            percentileIndex = 0;
        }
        Collections.sort(avgLatencies);
        return avgLatencies.get(percentileIndex);
    }

    /**
     * Calculate the serviceInstance's weight. 归一化为100内的权重
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
            weight = serviceInstanceWeight > (maxWeight.get() / 10) ? serviceInstanceWeight
                : (maxWeight.get() / 10) + serviceInstanceWeight;
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
    protected int serviceInstanceHashCode(Server serviceInstance) {
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

    private void addLalbLatencyStats(Server server) {
        StarlightStatistics statistics = StarlightStatsManager.getOrCreateStatsByHostPort(server.getHostPort());
        // putIfAbsent
        statistics.registerStats(LALB_STATS_KEY, new LalbLatencyStats());
    }

    /**
     * For test
     * 
     * @return
     */
    protected WeightTreeNode<Server> getWeightTree() {
        return weightTree;
    }
}
