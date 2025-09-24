package org.vertex.flow.util.operator;

import jakarta.annotation.Nullable;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.operator.IOperator;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OperatorUtil {

    private OperatorUtil() {

    }

    public static List<Method> findOperatorExecuteMethods(Class<? extends IOperator> operatorClass) {
        List<Method> operatorExecuteMethods = Arrays.stream(operatorClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OperatorExecute.class))
                .toList();
        return operatorExecuteMethods;
    }

    @Nullable
    public static Method findOperatorExecuteMethod(Class<? extends IOperator> operatorClass) {
        return Arrays.stream(operatorClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OperatorExecute.class))
                .findFirst()
                .orElse(null);
    }

    public static Parameter[] getParametersForOperatorExecuteMethod(Class<? extends IOperator> operatorClass) throws NullPointerException{
        return Objects.requireNonNull(findOperatorExecuteMethod(operatorClass)).getParameters();
    }

    public static  Class<?> getReturnTypeForOperatorExecuteMethod(Class<? extends IOperator> operatorClass) throws NullPointerException {
        return Objects.requireNonNull(findOperatorExecuteMethod(operatorClass)).getReturnType();
    }
}
