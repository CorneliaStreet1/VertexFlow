package org.vertex.flow.operator;

import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.context.GraphContextHolder;

@Operator
public class ContextReadOperator implements IOperator {

    @OperatorExecute
    public void execute() {
        Integer val = GraphContextHolder.getValue("x");
        if (!Integer.valueOf(42).equals(val)) {
            throw new IllegalStateException("Context value mismatch");
        }
        System.out.println("Context value = " + val);
    }
}
