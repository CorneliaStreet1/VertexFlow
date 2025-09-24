package org.vertex.flow.graph.builder;

import jakarta.annotation.Nonnull;
import org.vertex.flow.domain.model.Graph;
import org.vertex.flow.domain.model.Node;
import org.vertex.flow.operator.IOperator;


public class GraphWrapper {

    private final Graph graph;

    public GraphWrapper() {
        this.graph = new Graph();
    }


    public GraphNodeWrapper addNode(@Nonnull Class<? extends IOperator> operatorClazz) {
        Node node = this.graph.addNode(operatorClazz);
        GraphNodeWrapper graphNodeWrapper = new GraphNodeWrapper(node);
        return graphNodeWrapper;
    }

    public GraphNodeWrapper addNode(@Nonnull Class<? extends IOperator> operatorClazz, @Nonnull String suffix) {
        Node node = this.graph.addNode(operatorClazz, suffix);
        GraphNodeWrapper graphNodeWrapper = new GraphNodeWrapper(node);
        return graphNodeWrapper;
    }

}
