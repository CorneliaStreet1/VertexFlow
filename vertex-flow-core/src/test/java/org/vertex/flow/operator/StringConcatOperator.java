package org.vertex.flow.operator;

import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.annotation.OperatorFallBack;

@Operator
public class StringConcatOperator implements IOperator {

    @OperatorExecute
    public String execute() {
        return "startNode";
    }

    @OperatorFallBack
    public String fallback() {
        return "[fallback]startNode";
    }
}
