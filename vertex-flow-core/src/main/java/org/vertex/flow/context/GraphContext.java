package org.vertex.flow.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class GraphContext {
    private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();

    public void put(@Nonnull String key, Object value) {
        data.put(key, value);
    }

    @Nullable
    public <T> T get(@Nonnull String key) {
        return (T) data.get(key);
    }

    @Nullable
    public <T> T getOrDefault(@Nonnull String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }

    @Nullable
    public <T> T get(@Nonnull String key, Class<T> type) throws IllegalArgumentException {
        Objects.requireNonNull(key, "graph context-key is null");
        Object val = data.get(key);
        if (val == null) return null;
        if (!type.isInstance(val)) {
            throw new IllegalArgumentException("Type mismatch for key=" + key);
        }
        return (T) val;
    }

    @Nullable
    public <T> T getOrDefault(@Nonnull String key, Class<T> type, T defaultValue) throws IllegalArgumentException {
        Objects.requireNonNull(key, "graph context-key is null");
        Object val = data.get(key);
        if (val == null) {
            return defaultValue;
        }
        if (!type.isInstance(val)) {
            throw new IllegalArgumentException("Type mismatch for key=" + key);
        }
        return (T) val;
    }

    public void remove(@Nonnull String key) {
        data.remove(key);
    }

    public void clear() {
        data.clear();
    }
}
