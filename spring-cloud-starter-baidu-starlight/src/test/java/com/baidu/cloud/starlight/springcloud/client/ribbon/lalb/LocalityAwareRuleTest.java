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

import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.baidu.cloud.starlight.springcloud.client.ribbon.TestLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2021/9/22.
 */
public class LocalityAwareRuleTest {

    @Test
    public void updateWeightTree100000() {
        updateWeightTree(100000);
    }

    @Test
    public void updateWeightTree1000() {
        updateWeightTree(1000);
    }

    public void updateWeightTree(int size) {
        // cost test
        ILoadBalancer loadBalancer = new TestLoadBalancer();

        LocalityAwareRule lalb = new LocalityAwareRule();
        lalb.setLoadBalancer(loadBalancer);

        List<Server> servers = new LinkedList<>();
        for (int i = 0; i < size; i++) { // 10000 -> 30ms, 100000 -> 214ms
            Server server = new Server("localhost", i);
            servers.add(server);
            LalbLatencyStats stats = new LalbLatencyStats();
            for (int j = 0; j < 10; j++) {
                stats.updateLatencyWindow((long) i + 1);
            }
            StarlightStatsManager.getOrCreateStatsByHostPort(server.getHostPort())
                .registerStats(LocalityAwareRule.LALB_STATS_KEY, stats);
        }

        loadBalancer.addServers(servers);
        for (int i = 0; i < 1000; i++) {
            lalb.updateWeightTree();
        }

        StarlightStatistics statistics = StarlightStatsManager.getStatsByHostPort(servers.get(10).getHostPort());
        assertNotNull(statistics);
        assertNotNull(statistics.discoverStats(LocalityAwareRule.LALB_STATS_KEY));

        WeightTreeNode<Server> weightTree = lalb.getWeightTree();
        assertNotNull(weightTree);
    }

    @Test
    public void weightTreeNodes() {
        LocalityAwareRule lalb = new LocalityAwareRule();

        List<Server> servers = new LinkedList<>();

        for (int i = 0; i < 100000; i++) {
            Server server = new Server("localhost", i);
            servers.add(server);
            LalbLatencyStats stats = new LalbLatencyStats();
            for (int j = 0; j < 10; j++) {
                stats.updateLatencyWindow((long) i + 1);
            }
            StarlightStatsManager.getOrCreateStatsByHostPort(server.getHostPort())
                .registerStats(LocalityAwareRule.LALB_STATS_KEY, stats);
        }

        Queue<WeightTreeNode<Server>> weightTreeNodes = lalb.weightTreeNodes(servers);
        assertEquals(servers.size(), weightTreeNodes.size());
    }

    @Test
    public void latency90Percentile() {
        LocalityAwareRule lalb = new LocalityAwareRule();

        List<Long> avgLatencies = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            avgLatencies.add((long) i);
        }
        Long avg90 = lalb.latency90Percentile(avgLatencies);

        assertEquals(89, avg90.intValue());
    }

    @Test
    public void choose() {
        ILoadBalancer loadBalancer = new TestLoadBalancer();

        LocalityAwareRule lalb = new LocalityAwareRule();
        lalb.setLoadBalancer(loadBalancer);

        List<Server> servers = new LinkedList<>();
        for (int i = 0; i < 1000; i++) {
            Server server = new Server("localhost", i);
            servers.add(server);
            LalbLatencyStats stats = new LalbLatencyStats();
            for (int j = 0; j < 10; j++) {
                stats.updateLatencyWindow((long) i + j * 10);
            }
            StarlightStatsManager.getOrCreateStatsByHostPort(server.getHostPort())
                .registerStats(LocalityAwareRule.LALB_STATS_KEY, stats);
        }

        loadBalancer.addServers(servers);
        lalb.updateWeightTree();

        Map<String, Long> hostCount = new HashMap<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            threadPool.execute(() -> {
                for (int d = 0; d < 10000; d++) {
                    Server server = lalb.choose("default");
                    synchronized (LocalityAwareRuleTest.class) {
                        hostCount.compute(server.getHostPort(), (k, v) -> {
                            if (v == null) {
                                return 1L;
                            } else {
                                return v + 1L;
                            }
                        });
                    }
                }
                countDownLatch.countDown();
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(10 * 10000, sumCount(hostCount));
        System.out.println(hostCount);
        System.out.println(hostCount.size());
        assertTrue(hostCount.get("localhost:0") > hostCount.get("localhost:200"));
        assertTrue(hostCount.get("localhost:200") > hostCount.get("localhost:500"));
        assertTrue(hostCount.get("localhost:500") > hostCount.get("localhost:900"));
    }

    @Test
    public void searchNode() {
        LocalityAwareRule lalb = new LocalityAwareRule();

        Queue<WeightTreeNode<Server>> weightTreeNodes = new LinkedList<>();

        long expectWeight = 0;
        for (int i = 0; i < 1000; i++) {
            long weight = 1000 - i;
            WeightTreeNode<Server> treeNode = new WeightTreeNode<>(i, weight, new Server("localhost", i));
            weightTreeNodes.add(treeNode);
            expectWeight += weight;
        }

        WeightTreeNode<Server> weightTree = lalb.generateWeightTreeByNodes(weightTreeNodes);
        assertEquals(expectWeight, weightTree.getWeight());

        long totalWeight = weightTree.getWeight();

        int reqCount = 1000000;
        Map<String, Long> searchResult = new HashMap<>();
        for (int i = 0; i < reqCount; i++) {
            Server server =
                lalb.searchNode(weightTree, ThreadLocalRandom.current().nextLong(totalWeight)).getNodeEntity();
            searchResult.compute(server.getHostPort(), (k, v) -> {
                if (v == null) {
                    return 1L;
                } else {
                    return v + 1L;
                }
            });
        }

        long sumCount = 0;
        for (Long count : searchResult.values()) {
            sumCount += count;
        }
        assertEquals(reqCount, sumCount);
        System.out.println(searchResult);

        assertTrue(searchResult.get("localhost:0") > searchResult.get("localhost:50"));
        assertTrue(searchResult.get("localhost:50") > searchResult.get("localhost:100"));
        assertTrue(searchResult.get("localhost:100") > searchResult.get("localhost:500"));
        assertTrue(searchResult.get("localhost:500") > searchResult.get("localhost:900"));

    }

    @Test
    public void searchNodeReverseTreeWeight() {
        LocalityAwareRule lalb = new LocalityAwareRule();

        Queue<WeightTreeNode<Server>> weightTreeNodes = new LinkedList<>();

        long expectWeight = 0;
        for (int i = 0; i < 1000; i++) {
            long weight = 1000 - (1000 - i) + 1;
            WeightTreeNode<Server> treeNode = new WeightTreeNode<>(i, weight, new Server("localhost", i));
            weightTreeNodes.add(treeNode);
            expectWeight += weight;
        }

        WeightTreeNode<Server> weightTree = lalb.generateWeightTreeByNodes(weightTreeNodes);
        assertEquals(expectWeight, weightTree.getWeight());

        long totalWeight = weightTree.getWeight();

        int reqCount = 1000000;
        Map<String, Long> searchResult = new HashMap<>();
        for (int i = 0; i < reqCount; i++) {
            Server server =
                lalb.searchNode(weightTree, ThreadLocalRandom.current().nextLong(totalWeight)).getNodeEntity();
            searchResult.compute(server.getHostPort(), (k, v) -> {
                if (v == null) {
                    return 1L;
                } else {
                    return v + 1L;
                }
            });
        }

        long sumCount = 0;
        for (Long count : searchResult.values()) {
            sumCount += count;
        }
        assertEquals(reqCount, sumCount);
        System.out.println(searchResult);

        assertTrue(searchResult.get("localhost:10") < searchResult.get("localhost:50"));
        assertTrue(searchResult.get("localhost:50") < searchResult.get("localhost:100"));
        assertTrue(searchResult.get("localhost:100") < searchResult.get("localhost:500"));
        assertTrue(searchResult.get("localhost:500") < searchResult.get("localhost:900"));

    }

    @Test
    public void generateWeightTreeByNodes() {
        LocalityAwareRule lalb = new LocalityAwareRule();

        Queue<WeightTreeNode<Server>> weightTreeNodes = new LinkedList<>();

        int expectWeight = 0;
        for (int i = 0; i < 100; i++) {
            WeightTreeNode<Server> treeNode = new WeightTreeNode<>(i, i, new Server("localhost", i));
            weightTreeNodes.add(treeNode);
            expectWeight += i;
        }

        WeightTreeNode<Server> weightTree = lalb.generateWeightTreeByNodes(weightTreeNodes);
        assertEquals(expectWeight, weightTree.getWeight());
        assertTrue(weightTree.getChildSize() > 100);
    }

    private long sumCount(Map<String, Long> result) {
        long sumCount = 0;
        for (Long count : result.values()) {
            sumCount += count;
        }

        return sumCount;
    }

    @Test
    public void test() {
        int result = (int) ((100 * 0.1) / (90 * 0.1) * 100);
        System.out.println((100 * 0.1) / (90 * 0.1) * 100);
        System.out.println(result);

        long result2 = (long) ((100L * 0.1) / (90L * 0.1) * 100L);
        System.out.println((100L * 0.1) / (90L * 0.1) * 100L);
        System.out.println(result2);
    }

}