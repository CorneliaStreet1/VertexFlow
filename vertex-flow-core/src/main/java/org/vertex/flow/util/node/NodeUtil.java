package org.vertex.flow.util.node;

import jakarta.annotation.Nullable;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.domain.model.Node;
import org.vertex.flow.operator.IOperator;
import org.vertex.flow.util.operator.OperatorUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NodeUtil {
    private NodeUtil() {

    }

    public static List<Method> findOperatorExecuteMethods(Node node) {
        return OperatorUtil.findOperatorExecuteMethods(node.getOperator().getClass());
    }

    @Nullable
    public static Method findOperatorExecuteMethod(Node node) {
        return OperatorUtil.findOperatorExecuteMethod(node.getOperator().getClass());
    }

    public static Parameter[] getParametersForOperatorExecuteMethod(Node node) throws NullPointerException{
        return OperatorUtil.getParametersForOperatorExecuteMethod(node.getOperator().getClass());
    }

    public static  Class<?> getReturnTypeForOperatorExecuteMethod(Node node) throws NullPointerException {
        return OperatorUtil.getReturnTypeForOperatorExecuteMethod(node.getOperator().getClass());
    }
}
