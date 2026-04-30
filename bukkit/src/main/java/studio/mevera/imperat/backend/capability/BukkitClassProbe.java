package studio.mevera.imperat.backend.capability;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Reflection-only class-existence probe for {@link CapabilityResolver}.
 *
 * <p>Uses the OWNING PLUGIN's classloader so plugins that shade the
 * Imperat framework still see their server's runtime classes (paper-api
 * may be loaded from the server's bootstrap loader; the framework's
 * loader may not see it directly).</p>
 *
 * <p>Initialization is intentionally suppressed via
 * {@code Class.forName(name, false, loader)} — probing
 * {@code io.papermc.paper.command.brigadier.Commands} must NOT trigger
 * static-init on classpaths where its dependencies are partially
 * present. Cached per-name to avoid repeat reflection costs.</p>
 *
 * @since 4.0.0
 */
public final class BukkitClassProbe {

    private final ClassLoader classLoader;
    private final Map<String, Boolean> cache = new HashMap<>();

    public BukkitClassProbe(@NotNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static @NotNull BukkitClassProbe forPlugin(@NotNull Plugin plugin) {
        return new BukkitClassProbe(plugin.getClass().getClassLoader());
    }

    /**
     * @return {@code true} iff the named class is loadable from the
     * configured classloader without triggering its static initializer.
     */
    public boolean exists(@NotNull String fullyQualifiedName) {
        Boolean cached = cache.get(fullyQualifiedName);
        if (cached != null) {
            return cached;
        }
        boolean present;
        try {
            Class.forName(fullyQualifiedName, false, classLoader);
            present = true;
        } catch (Throwable ignored) {
            present = false;
        }
        cache.put(fullyQualifiedName, present);
        return present;
    }

    public @NotNull ClassLoader classLoader() {
        return classLoader;
    }
}
