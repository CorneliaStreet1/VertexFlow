package org.vertex.flow.util.spring;

import jakarta.annotation.Nonnull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 静态持有 Spring ApplicationContext，便于非 Bean 类获取 Spring Bean
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.context = applicationContext;
    }

    public static ApplicationContext getContext() {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext not initialized yet");
        }
        return context;
    }
}

