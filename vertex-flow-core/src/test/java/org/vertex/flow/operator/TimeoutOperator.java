package org.vertex.flow.operator;

import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;

@Operator
public class TimeoutOperator implements IOperator {

    @OperatorExecute
    public void execute() throws InterruptedException {
        Thread.sleep(5000); // 模拟超时
    }
}
