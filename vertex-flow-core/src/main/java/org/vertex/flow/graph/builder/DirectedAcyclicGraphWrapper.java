package org.vertex.flow.graph.builder;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.vertex.flow.annotation.OpInput;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.domain.exception.GraphConstructionException;
import org.vertex.flow.operator.IOperator;
import org.vertex.flow.spring.util.BeanUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * DAG包装类
 */

@NoArgsConstructor
@Data
public class DirectedAcyclicGraphWrapper {
    /**
     * Key: 节点的唯一Id
     * Value: 节点
     */
    private Map<String, GraphNodeWrapper> nodeId2NodeWrapper = Maps.newHashMap();

    /**
     * DAG节点之间的依赖关系是否已经解析
     */
    private boolean nextDependParsed = false;

    /**
     * 开始节点结合
     * 调度从这里开始出发
     */
    private Set<GraphNodeWrapper> startNodesWrapperSet = Sets.newHashSet();

    /**
     * 结束节点集合
     * 引擎执行过程中可以根据节点执行情况动态设置结束节点，需要使用线程安全的集合’
     * 每个节点执行完毕都要检查其是否是结束节点,如果是就需要将其移除
     */
    private Set<GraphNodeWrapper> endNodesWrapperSet =  Sets.newConcurrentHashSet();

    public GraphNodeWrapper addNode(@Nonnull Class<? extends IOperator> operatorClazz) {
        Objects.requireNonNull(operatorClazz, "class is null");
        String nodeId = operatorClazz.getSimpleName();
        if (nodeId2NodeWrapper.containsKey(nodeId)) {
            throw new GraphConstructionException("Duplicate node id " + nodeId);
        }
        IOperator operatorBean = BeanUtil.getBean(operatorClazz);
        if (operatorBean == null) {
            throw new GraphConstructionException("No bean found for class " + operatorClazz.getSimpleName());
        }

        GraphNodeWrapper node = new GraphNodeWrapper(operatorBean, nodeId);
        nodeId2NodeWrapper.put(nodeId, node);

        return node;
    }

    public GraphNodeWrapper addNode(@Nonnull Class<? extends IOperator> operatorClazz, @Nonnull String suffix) {
        Objects.requireNonNull(operatorClazz, "class is null");
        Objects.requireNonNull(suffix, "suffix is null");
        if (StringUtils.isBlank(suffix)) {
            throw new GraphConstructionException("Suffix should not be blank or empty or null: " + suffix);
        }
        String nodeId = operatorClazz.getSimpleName() + "." +  suffix;
        if (nodeId2NodeWrapper.containsKey(nodeId)) {
            throw new GraphConstructionException("Duplicate node id " + nodeId);
        }
        IOperator operatorBean = BeanUtil.getBean(operatorClazz);
        if (operatorBean == null) {
            throw new GraphConstructionException("No bean found for class " + operatorClazz.getSimpleName());
        }

        GraphNodeWrapper node = new GraphNodeWrapper(operatorBean, nodeId);
        nodeId2NodeWrapper.put(nodeId, node);

        return node;
    }


    public boolean isDAG() {
        Map<String, GraphNodeWrapper> nodeId2NodeWrapper = this.getNodeId2NodeWrapper();
        // 计算每个节点的入度
        Map<String, Integer> inDegree = new HashMap<>();
        for (Map.Entry<String, GraphNodeWrapper> entry : nodeId2NodeWrapper.entrySet()) {
            String nodeId = entry.getKey();
            GraphNodeWrapper nodeWrapper = entry.getValue();
            inDegree.put(nodeId, nodeWrapper.getPreDependNodes().size());
        }

        // 将入度为0的节点放入队列
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int visited = 0;
        while (!queue.isEmpty()) {
            String node = queue.poll();
            visited++;

            for (GraphNodeWrapper next : nodeId2NodeWrapper.get(node).getPreDependNodes()) {
                inDegree.put(next.getNodeId(), inDegree.get(next.getNodeId()) - 1);
                if (inDegree.get(next.getNodeId()) == 0) {
                    queue.add(next.getNodeId());
                }
            }
        }

        // 如果遍历完的节点数 == 图中节点数 → 无环
        return visited == inDegree.size();

    }

}
