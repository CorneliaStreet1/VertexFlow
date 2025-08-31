package org.vertex.flow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注算子的降级方法
 * 算子跳过或者异常时执行
 * 入参列表需要与{@link OperatorExecute}标注的方法一致
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperatorFallBack {

}
