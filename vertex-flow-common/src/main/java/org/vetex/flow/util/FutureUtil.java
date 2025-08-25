package org.vetex.flow.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Future工具类
 */
public class FutureUtil {

    private FutureUtil() {

    }

    public static <V> V getOrDefault(Future<V> future, V defaultValue) {
        try {
            return future.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static <V> V getOrDefault(Future<V> future, Supplier<V> defaultSupplier) {
       return getOrDefault(future, defaultSupplier.get());
    }

    public static <V> V getOrDefault(Future<V> future, V defaultValue, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static <V> V getOrDefault(Future<V> future, Supplier<V> defaultSupplier, Supplier<Long> timeOutSupplier, TimeUnit unit) {
        return getOrDefault(future, defaultSupplier.get(), timeOutSupplier.get(), unit);
    }

    public static <V> V getNowOrDefault(Future<V> future, V defaultValue) {
        try {
            if (future.isDone()) {
                return future.get();
            }
            return defaultValue;
        }
        catch (Exception e) {
            return defaultValue;
        }
    }
}
