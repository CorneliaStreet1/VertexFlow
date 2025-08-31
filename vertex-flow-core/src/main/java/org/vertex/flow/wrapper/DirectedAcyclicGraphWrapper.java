package org.vertex.flow.wrapper;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.vertex.flow.domain.exception.GraphConstructionException;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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



    public GraphNodeWrapper addNode(GraphNodeWrapper node) {

        Objects.requireNonNull(node, "node is null");
        Objects.requireNonNull(node.getNodeId(), "node id is null");

        if (nodeId2NodeWrapper.containsKey(node.getNodeId())) {
            throw new GraphConstructionException("Duplicate node id " + node.getNodeId());
        }
        nodeId2NodeWrapper.put(node.getNodeId(), node);

        return node;
    }

}
