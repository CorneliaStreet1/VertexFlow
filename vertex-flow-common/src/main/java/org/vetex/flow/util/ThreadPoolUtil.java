package org.vetex.flow.util;

import org.springframework.stereotype.Component;
import org.vetex.flow.domain.enums.ThreadPoolEnum;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * 线程池工具类
 */
public class ThreadPoolUtil {

    private ThreadPoolUtil() {

    }

    public static <V> CompletableFuture<V> submit(Supplier<V> supplier, ExecutorService executor) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public static <V> CompletableFuture<V> submit(Supplier<V> supplier, ThreadPoolEnum threadPoolEnum) {
        return submit(supplier, threadPoolEnum.getExecutor());
    }

}
