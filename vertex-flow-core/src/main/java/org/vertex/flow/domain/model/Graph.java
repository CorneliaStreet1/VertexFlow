package org.vertex.flow.domain.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.vertex.flow.domain.exception.GraphConstructionException;
import org.vertex.flow.operator.IOperator;
import org.vertex.flow.util.spring.util.BeanUtil;

import java.util.*;

/**
 * DAG包装类
 */

@NoArgsConstructor
@Getter
@Setter
public class Graph {
    /**
     * Key: 节点的唯一Id
     * Value: 节点
     */
    private Map<String, Node> nodeId2Node = Maps.newHashMap();

    /**
     * DAG节点之间的依赖关系是否已经解析
     */
    private boolean nextDependParsed = false;

    /**
     * 开始节点结合
     * 调度从这里开始出发
     */
    private Set<Node> startNodesSet = Sets.newHashSet();

    /**
     * 结束节点集合
     * 引擎执行过程中可以根据节点执行情况动态设置结束节点，需要使用线程安全的集合’
     * 每个节点执行完毕都要检查其是否是结束节点,如果是就需要将其移除
     */
    private Set<Node> endNodesSet =  Sets.newConcurrentHashSet();

    public Node addNode(@Nonnull Class<? extends IOperator> operatorClazz) {
        Objects.requireNonNull(operatorClazz, "class is null");
        String nodeId = operatorClazz.getSimpleName();
        if (nodeId2Node.containsKey(nodeId)) {
            throw new GraphConstructionException("Duplicate node id " + nodeId);
        }
        IOperator operatorBean = BeanUtil.getBean(operatorClazz);
        if (operatorBean == null) {
            throw new GraphConstructionException("No bean found for class " + operatorClazz.getSimpleName());
        }

        Node node = new Node(operatorBean, nodeId);
        nodeId2Node.put(nodeId, node);

        return node;
    }

    public Node addNode(@Nonnull Class<? extends IOperator> operatorClazz, @Nonnull String suffix) {
        Objects.requireNonNull(operatorClazz, "class is null");
        Objects.requireNonNull(suffix, "suffix is null");
        if (StringUtils.isBlank(suffix)) {
            throw new GraphConstructionException("Suffix should not be blank or empty or null: " + suffix);
        }
        String nodeId = operatorClazz.getSimpleName() + "." +  suffix;
        if (nodeId2Node.containsKey(nodeId)) {
            throw new GraphConstructionException("Duplicate node id " + nodeId);
        }
        IOperator operatorBean = BeanUtil.getBean(operatorClazz);
        if (operatorBean == null) {
            throw new GraphConstructionException("No bean found for class " + operatorClazz.getSimpleName());
        }

        Node node = new Node(operatorBean, nodeId);
        nodeId2Node.put(nodeId, node);

        return node;
    }

}
