package org.vertex.flow.operator;

import org.vertex.flow.annotation.OpInput;
import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.annotation.OperatorFallBack;

@Operator
public class SumOperator implements IOperator {

    @OperatorExecute
    public Integer execute(@OpInput Integer x, @OpInput Integer y) {
        return x + y;
    }

    @OperatorFallBack
    public Integer fallback(@OpInput Integer x, @OpInput Integer y) {
        return 0;
    }
}

