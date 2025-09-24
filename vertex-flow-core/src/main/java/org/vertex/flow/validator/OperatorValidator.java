package org.vertex.flow.validator;

import org.apache.commons.collections4.CollectionUtils;
import org.vertex.flow.annotation.OpInput;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.annotation.OperatorFallBack;
import org.vertex.flow.domain.exception.OperatorCheckException;
import org.vertex.flow.operator.IOperator;
import org.vertex.flow.util.operator.OperatorUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class OperatorValidator {

    public static void validateOperator(Object operator) throws OperatorCheckException {
        if (operator == null) {
            throw new OperatorCheckException("operator is null");
        }

        if (!(operator instanceof IOperator op)) {
            throw new OperatorCheckException("Operator object must be of type IOperator");
        }

        validateOperator(op);
    }

    private static void validateOperator(IOperator operator) throws OperatorCheckException {
        Class<? extends IOperator> operatorClass = operator.getClass();

        // 检查  @OperatorExecute 方法
        checkOperatorExecuteMethod(operatorClass);

        // 降级方法检查
        checkFallBackMethod(operatorClass);
    }

    /**
     * 1.有且仅有一个方法被 @OperatorFallBack 注解标记
     * 2.此方法必须是 public 的
     * 3.fallback 方法的 参数签名与主方法兼容（顺序、数量、类型相同或可兼容）
     * 4.返回类型与主方法一致或可兼容。
     */
    private static void checkFallBackMethod(Class<? extends IOperator> operatorClass) throws OperatorCheckException {
        List<Method> fallBackMethods = Arrays.stream(operatorClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OperatorFallBack.class))
                .toList();
        if (CollectionUtils.isEmpty(fallBackMethods)) {
            return;
        }

        if (fallBackMethods.size() != 1) {
            throw new OperatorCheckException("Multiple @OperatorFallBack methods found");
        }

        Method fallBackMethod = fallBackMethods.getFirst();
        if (!Modifier.isPublic(fallBackMethod.getModifiers())) {
            throw new OperatorCheckException("@OperatorFallBack method is not public");
        }

        // 此前已经检查过了,这里一定能取到
        Method operatorExecuteMethod = OperatorUtil.findOperatorExecuteMethods(operatorClass).getFirst();
        Class<?>[] mainParams = operatorExecuteMethod.getParameterTypes();
        Class<?>[] fallbackParams = fallBackMethod.getParameterTypes();

        if (mainParams.length != fallbackParams.length) {
            throw new OperatorCheckException("Fallback method parameter count mismatch");
        }

        for (int i = 0; i < mainParams.length; i++) {
            if (!mainParams[i].isAssignableFrom(fallbackParams[i])) {
                throw new OperatorCheckException("Fallback method parameter type mismatch at index " + i);
            }
        }

        if (!operatorExecuteMethod.getReturnType().isAssignableFrom(fallBackMethod.getReturnType())) {
            throw new OperatorCheckException("Fallback method return type mismatch");
        }
    }

    /**
     * 1.有且仅有一个方法被 @OperatorExecute 注解标记
     * 2.方法必须是 public 的
     * 3. 方法的每个入参都有且仅有一个 @OpInput 注解
     */
    private static void checkOperatorExecuteMethod(Class<? extends IOperator> operatorClass) throws OperatorCheckException {
        List<Method> operatorExecuteMethods = OperatorUtil.findOperatorExecuteMethods(operatorClass);

        if (CollectionUtils.isEmpty(operatorExecuteMethods)) {
            throw new OperatorCheckException("No @OperatorExecute method found" + operatorClass.getSimpleName());
        }
        if (operatorExecuteMethods.size() != 1) {
            throw new OperatorCheckException("Multiple @OperatorExecute methods found" + operatorClass.getSimpleName());
        }

        Method executeMethod = operatorExecuteMethods.getFirst();
        if (!Modifier.isPublic(executeMethod.getModifiers())) {
            throw new OperatorCheckException("@OperatorExecute method is not public");
        }

        long invalidParamCount = Arrays.stream(executeMethod.getParameters())
                .filter(parameter -> !parameter.isAnnotationPresent(OpInput.class))
                .count();
        if (invalidParamCount > 0) {
            throw new OperatorCheckException("@OperatorExecute method has invalid parameter without @OpInput: " + invalidParamCount);
        }
    }
}
