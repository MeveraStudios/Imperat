package studio.mevera.imperat.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Registry<K, V> {

    private final Map<K, V> data;

    public Registry(Supplier<Map<K, V>> data) {
        this(null, null, data);
    }

    public Registry() {
        this(HashMap::new);
    }

    public Registry(@Nullable K key, @Nullable V value, Supplier<Map<K, V>> data) {
        this.data = data.get();
        if (key != null && value != null) {
            this.data.put(key, value);
        }
    }

    public Optional<V> getData(K key) {
        return Optional.ofNullable(data.get(key));
    }

    public Registry<K, V> setData(K key, V value) {
        data.put(key, value);
        return this;
    }

    public void updateData(K key, Consumer<V> valueUpdater) {
        data.computeIfPresent(key, (k, v) -> {
            valueUpdater.accept(v);
            return v;
        });
    }

    public Optional<V> search(BiPredicate<K, V> predicate) {
        for (Map.Entry<K, V> entry : data.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                return Optional.ofNullable(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public Registry<K, V> addAll(Registry<K, V> registry) {
        this.data.putAll(registry.data);
        return this;
    }

    public Collection<? extends V> getAll() {
        return data.values();
    }

    public Iterable<? extends K> getKeys() {
        return data.keySet();
    }

    public void removeData(K key) {
        data.remove(key);
    }

    public void update(K key, Function<? super V, ? extends V> updater) {
        data.compute(key, (k, v) -> updater.apply(v));
    }

    public void updateIfPresent(K key, Function<? super V, ? extends V> updater) {
        data.computeIfPresent(key, (k, v) -> updater.apply(v));
    }

    public void updateIfAbsent(K key, Function<? super K, ? extends V> updater) {
        data.computeIfAbsent(key, updater);
    }

    public int size() {
        return data.size();
    }

    public Map<K, V> getMap() {
        return this.data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Registry<?, ?> registry)) {
            return false;
        }
        return Objects.equals(data, registry.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

}
