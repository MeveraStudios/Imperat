package studio.mevera.imperat.config.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.providers.DependencySupplier;
import studio.mevera.imperat.util.Registry;

import java.lang.reflect.Type;

/**
 * Typed wrapper around {@link Registry} for dependency-injection suppliers.
 * Exists so the public registry surface mirrors the rest of the config
 * (one registry per concern) instead of leaking the generic {@code Registry}
 * type into {@code ImperatConfigImpl}'s field block.
 */
public final class DependencyRegistry {

    private final Registry<Type, DependencySupplier> backing = new Registry<>();

    public void register(@NotNull Type type, @NotNull DependencySupplier supplier) {
        backing.setData(type, supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolve(@NotNull Type type) {
        return (T) backing.getData(type).map(DependencySupplier::get).orElse(null);
    }

    public boolean has(@NotNull Type type) {
        return backing.getData(type).isPresent();
    }
}
