package org.vertex.flow.validator;

import org.vertex.flow.annotation.Operator;
import org.vertex.flow.domain.exception.GraphCheckException;
import org.vertex.flow.domain.exception.NodeCheckException;
import org.vertex.flow.domain.exception.OperatorCheckException;
import org.vertex.flow.domain.model.Graph;
import org.vertex.flow.domain.model.Node;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class GraphValidator {

    private GraphValidator() {

    }

    public static void validateGraph(Graph graph) throws GraphCheckException, NodeCheckException, OperatorCheckException {
        // 检查图结构的合法性
        if (!isDAG(graph)) {
            throw new GraphCheckException("Graph is not a valid DAG");
        }
        // 检查图中每个节点的合法性
        graph.getNodeId2Node().values().forEach(NodeValidator::validateNode);
    }




    public static boolean isDAG(Graph graph) {
        Map<String, Node> nodeId2NodeWrapper = graph.getNodeId2Node();
        // 计算每个节点的入度
        Map<String, Integer> inDegree = new HashMap<>();
        for (Map.Entry<String, Node> entry : nodeId2NodeWrapper.entrySet()) {
            String nodeId = entry.getKey();
            Node nodeWrapper = entry.getValue();
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

            for (Node next : nodeId2NodeWrapper.get(node).getNextNodes()) {
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
