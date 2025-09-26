package org.vertex.flow.operator;

import org.vertex.flow.annotation.OpInput;
import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;

@Operator
public class PrintOperator implements IOperator {

    @OperatorExecute
    public void execute(@OpInput Object input) {
        System.out.println("PrintOperator => " + input);
    }
}
