package org.vertex.flow.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER}) // 仅可以标注在方法入参上
@Retention(RetentionPolicy.RUNTIME)// 需要在运行时可用, 以支持反射
public @interface OpInput {

}
