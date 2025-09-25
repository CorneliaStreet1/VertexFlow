package org.vertex.flow.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@SuppressWarnings("unchecked")
public class GraphContextHolder {
    private static final TransmittableThreadLocal<GraphContext> CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    /**
     *
     * @param context 设置当前 DAG 的上下文
     */
    public static void setContext(GraphContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     *
     * @return 获取当前 DAG 的上下文
     */
    public static GraphContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /** 清理上下文 */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 清理当前 DAG 的上下文
     */
    public static void clearGraphContext() {
        CONTEXT_HOLDER.get().clear();
    }

    @Nullable
    public static <T> T getValue(@Nonnull String key, @Nonnull Class<T> valueType)  throws IllegalArgumentException {
        return (T) getContext().get(key, valueType);
    }

    @Nullable
    public  static <T> T getValue(@Nonnull String key) {
        return (T) getContext().get(key);
    }

    @Nullable
    public static  <T> T getValueOrDefault(@Nonnull String key, T defaultValue) {
        return (T) getContext().getOrDefault(key, defaultValue);
    }

    @Nullable
    public static  <T> T getValueOrDefault(@Nonnull String key,Class<T> clz, T defaultValue)  throws IllegalArgumentException {
        return (T) getContext().getOrDefault(key, clz,defaultValue);
    }


    public static void removeValue(@Nonnull String key) {
        CONTEXT_HOLDER.get().remove(key);
    }

    public static <T> void setValue(@Nonnull String key, T val) {
        getContext().put(key, val);
    }
}
