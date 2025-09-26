package org.vertex.flow.operator;

import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.context.GraphContext;
import org.vertex.flow.context.GraphContextHolder;

@Operator
public class ContextWriteOperator implements IOperator {

    @OperatorExecute
    public void execute() {
        GraphContextHolder.setValue("x", 42);
    }
}

