package studio.mevera.imperat.config;

import org.jetbrains.annotations.Nullable;

public record BehaviouralOption<T>(BehaviouralOptionKey key, Class<T> valueType, @Nullable T value, @Nullable T defaultValue) {

    public BehaviouralOption<T> reset() {
        return new BehaviouralOption<T>(key, valueType, defaultValue, defaultValue);
    }

    public BehaviouralOption<T> setValue(T value) {
        return new BehaviouralOption<>(key, valueType, value, defaultValue);
    }
}
