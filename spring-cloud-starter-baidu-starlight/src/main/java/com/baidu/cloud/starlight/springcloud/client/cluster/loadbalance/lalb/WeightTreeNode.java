package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.lalb;

/**
 * 权重树节点
 * Created by liuruisen on 2020/10/10.
 */
public class WeightTreeNode<T> {

    private int hashId;
    private long weight;
    private WeightTreeNode<T> parentNode;
    private WeightTreeNode<T> leftNode;
    private WeightTreeNode<T> rightNode;
    private T nodeEntity;
    private long childSize;

    public WeightTreeNode() {

    }

    public WeightTreeNode(int hashId, long weight) {
        this.hashId = hashId;
        this.weight = weight;
    }

    public WeightTreeNode(int hashId, long weight, T nodeEntity) {
        this.hashId = hashId;
        this.weight = weight;
        this.nodeEntity = nodeEntity;
    }

    public long getChildSize() {
        return childSize;
    }

    public void setChildSize(long childSize) {
        this.childSize = childSize;
    }

    public int getHashId() {
        return hashId;
    }

    public void setHashId(int hashId) {
        this.hashId = hashId;
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public WeightTreeNode<T> getParentNode() {
        return parentNode;
    }

    public void setParentNode(WeightTreeNode<T> parentNode) {
        this.parentNode = parentNode;
    }

    public WeightTreeNode<T> getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(WeightTreeNode<T> leftNode) {
        this.leftNode = leftNode;
    }

    public WeightTreeNode<T> getRightNode() {
        return rightNode;
    }

    public void setRightNode(WeightTreeNode<T> rightNode) {
        this.rightNode = rightNode;
    }

    public T getNodeEntity() {
        return nodeEntity;
    }

    public void setNodeEntity(T nodeEntity) {
        this.nodeEntity = nodeEntity;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WeightTreeNode{");
        sb.append("hashId=").append(hashId);
        sb.append(", weight=").append(weight);
        sb.append(", childSize=").append(childSize);
        if (nodeEntity != null) {
            sb.append(", nodeEntity=").append(nodeEntity);
        }
        sb.append('}');
        return sb.toString();
    }
}
