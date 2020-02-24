/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.client.loadbalance;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.baidu.brpc.CommunicationSpiManager;
import com.baidu.brpc.GovernanceSpiManager;
import com.baidu.brpc.RpcOptionsUtils;
import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.client.CommunicationOptions;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.loadbalance.FairStrategy;
import org.junit.Assert;
import org.junit.Test;

public class FairStrategyTest extends FairStrategy {

    @Test
    public void test1() {
        Queue<Node> leafNodes = new LinkedList<Node>();
        Node node1 = new Node(1, 8, true);
        Node node2 = new Node(1, 12, true);
        Node node3 = new Node(1, 10, true);
        leafNodes.add(node1);
        leafNodes.add(node2);
        leafNodes.add(node3);
        Node root = generateWeightTreeByLeafNodes(leafNodes);
        Node selected = searchNode(root, 6);
        Assert.assertSame(node1, selected);
        selected = searchNode(root, 18);
        Assert.assertSame(node2, selected);
        selected = searchNode(root, 22);
        Assert.assertSame(node3, selected);
    }

    @Test
    public void test2() {
        Queue<Node> leafNodes = new LinkedList<Node>();
        Node node1 = new Node(1, 8, true);
        Node node2 = new Node(1, 12, true);
        Node node3 = new Node(1, 8, true);
        Node node4 = new Node(1, 2, true);
        leafNodes.add(node1);
        leafNodes.add(node2);
        leafNodes.add(node3);
        leafNodes.add(node4);
        Node root = generateWeightTreeByLeafNodes(leafNodes);
        Node selected = searchNode(root, 22);
        Assert.assertSame(node3, selected);
        selected = searchNode(root, 30);
        Assert.assertSame(node4, selected);
    }

    @Test
    public void testSelectInstance() {
        CommunicationSpiManager.getInstance().loadAllExtensions("utf8");
        GovernanceSpiManager.getInstance().loadAllExtensions();
        ServiceInstance serviceInstance1 = new ServiceInstance("127.0.0.1", 8000);
        serviceInstance1.setServiceName("EchoService");
        CommunicationClient instance1 = new CommunicationClient(serviceInstance1,
                RpcOptionsUtils.getCommunicationOptions(), null);

        ServiceInstance serviceInstance2 = new ServiceInstance("127.0.0.1", 8001);
        serviceInstance2.setServiceName("EchoService");
        CommunicationClient instance2 = new CommunicationClient(serviceInstance2,
                RpcOptionsUtils.getCommunicationOptions(), null);

        ServiceInstance serviceInstance3 = new ServiceInstance("127.0.0.1", 8002);
        serviceInstance3.setServiceName("EchoService");
        CommunicationClient instance3 = new CommunicationClient(serviceInstance3,
                RpcOptionsUtils.getCommunicationOptions(), null);

        Queue<Node> leafNodes = new LinkedList<Node>();
        leafNodes.add(new Node(instance1.hashCode(), 8, true, instance1));
        leafNodes.add(new Node(instance2.hashCode(), 12, true, instance2));
        leafNodes.add(new Node(instance3.hashCode(), 10, true, instance3));
        Node root = generateWeightTreeByLeafNodes(leafNodes);
        treeContainer.add(0, root);

        CopyOnWriteArrayList<CommunicationClient> instances = new CopyOnWriteArrayList<CommunicationClient>();
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);

        Set<CommunicationClient> selectedInstances = new HashSet<CommunicationClient>();
        selectedInstances.add(instance2);
        CommunicationClient instance = selectInstance(null, instances, selectedInstances);
        Assert.assertTrue(instance.getServiceInstance().getPort() != instance2.getServiceInstance().getPort());

        selectedInstances.add(instance3);
        instance = selectInstance(null, instances, selectedInstances);
        Assert.assertTrue(instance.getServiceInstance().getPort() == instance1.getServiceInstance().getPort());
    }
}
