package org.vertex.flow.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE}) // 仅可以标注在类、接口、枚举
@Retention(RetentionPolicy.RUNTIME)// 需要在运行时可用
@Component
public @interface Operator {
    
}
