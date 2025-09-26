package org.vertex.flow.operator;

import org.vertex.flow.annotation.OpInput;
import org.vertex.flow.annotation.Operator;
import org.vertex.flow.annotation.OperatorExecute;

import java.util.Arrays;
import java.util.List;

@Operator
public class TokenizeOperator implements IOperator {

    @OperatorExecute
    public List<String> execute(@OpInput String text) {
        return Arrays.asList(text.split("\\s+"));
    }
}
