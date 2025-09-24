package org.vertex.flow.graph.builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.vertex.flow.domain.model.Node;

@Getter
@AllArgsConstructor
public class GraphNodeWrapper {

    private final Node node;

    public GraphNodeWrapper depend(GraphNodeWrapper nodeWrapper) {
        this.node.addPreNode(nodeWrapper.node);
        return this;
    }

    public GraphNodeWrapper depends(GraphNodeWrapper... nodeWrappers) {
        for (GraphNodeWrapper nodeWrapper : nodeWrappers) {
            this.depend(nodeWrapper);
        }
        return this;
    }

}
