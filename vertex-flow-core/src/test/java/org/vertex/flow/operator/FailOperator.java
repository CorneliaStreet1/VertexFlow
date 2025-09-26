package org.vertex.flow.operator;

import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;

@Operator
public class FailOperator implements IOperator {

    @OperatorExecute
    public void execute() {
        throw new RuntimeException("FailOperator throws exception intentionally");
    }
}

