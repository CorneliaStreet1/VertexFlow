package org.vertex.flow.domain.wrapper;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.vertex.flow.operator.IOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dag的节点包装类
 * 一个节点是一个算子的实例以及其他的上下文信息
 * 多个节点可以是同一个算子的实例化, 但是这些节点的Id必须唯一
 */
@Data
@Accessors(chain=true)
public class GraphNodeWrapper {

    /**
     * 构成这个节点的算子
     */
    private IOperator operator;

    /**
     * 节点的唯一Id,默认是算子的名字
     */
    private String nodeId;

    /**
     * 节点的入度
     */
    private AtomicInteger inDegree;

    /**
     * 节点的出度
     */
    private AtomicInteger outDegree;


    /**
     * 前置依赖节点的集合
     */
    private Set<GraphNodeWrapper> preDependNodes;

    /**
     * 后置节点的集合
     */
    private Set<GraphNodeWrapper> nextNodes;

    /**
     * 节点是否已经被初始化过了
     * 这里的初始化是指它的前驱和后继都解析完成了
     */
    private boolean init;

    /**
     * 前置依赖节点的节点ID的列表(也即算子入参)
     * 需要与{@link GraphNodeWrapper#dependOn}调用的顺序保持一致
     * 或者与{@link GraphNodeWrapper#dependOns}中可变参数的出现顺序保持一致
     */
    private List<String> operatorInputNodeIds;


    // 防止外部调用无参构造器构造非法节点
    private GraphNodeWrapper() {

    }

    public GraphNodeWrapper(@Nonnull IOperator operator, @Nonnull String nodeId) {
        Objects.requireNonNull(operator, "operator cannot be null");
        Objects.requireNonNull(nodeId, "nodeId cannot be null");

        this.operator = operator;
        this.nodeId = nodeId;
        initFields();
    }

    private void initFields() {
        preDependNodes = Sets.newHashSet();
        nextNodes = Sets.newHashSet();
        inDegree = new AtomicInteger(0);
        outDegree = new AtomicInteger(0);
        operatorInputNodeIds = Lists.newArrayList();
        init = false;
    }

    public GraphNodeWrapper dependOn(GraphNodeWrapper node) {
        Objects.requireNonNull(node, "node cannot be null");
        Objects.requireNonNull(node.getNodeId(), "node id cannot be null");

        this.preDependNodes.add(node);
        this.operatorInputNodeIds.addLast(node.getNodeId());

        return this;
    }

    public GraphNodeWrapper dependOns(GraphNodeWrapper... nodes) {
        Objects.requireNonNull(nodes, "nodes cannot be null");
        if (nodes.length == 0) {
            return this;
        }
        for (GraphNodeWrapper node : nodes) {
            this.dependOn(node);
        }

        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GraphNodeWrapper that = (GraphNodeWrapper) o;

        return new EqualsBuilder().append(nodeId, that.nodeId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(nodeId).toHashCode();
    }
}
