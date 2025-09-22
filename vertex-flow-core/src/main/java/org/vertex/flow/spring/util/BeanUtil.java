package org.vertex.flow.spring.util;


import org.springframework.context.ApplicationContext;
import org.vertex.flow.spring.SpringContextHolder;

public class BeanUtil {

    /**
     * 获取 Spring 容器中指定类型的 Bean
     * @param clazz Bean 类型
     * @param <T> 泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        ApplicationContext context = SpringContextHolder.getContext();
        return context.getBean(clazz);
    }
}

