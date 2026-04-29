package studio.mevera.imperat.backend;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.command.Command;

/**
 * Runtime SPI that abstracts the two Bukkit/Paper integration paths:
 * <ul>
 *   <li>{@code ModernPaperBackend} — modern Paper 1.21.4+ with Paper's stable
 *       Brigadier API and lifecycle event registrar (no NMS, no Commodore).</li>
 *   <li>{@code LegacyBackend} — Spigot / pre-1.21 Paper with reflection-based
 *       command-map registration and no Brigadier integration. Tab completion
 *       flows through the {@code Command#tabComplete} path.</li>
 * </ul>
 *
 * <p>{@code BukkitImperat} probes the runtime classpath at construction time
 * and instantiates the appropriate backend — plugin authors choose nothing.</p>
 *
 * <p>Implementations live in {@code backend.modern} and {@code backend.legacy}
 * sub-packages. The framework only ships those two; the interface is left
 * non-sealed because Java's unnamed-module sealing forbids cross-package
 * permits, and there is no value in forcing a named module just for this.</p>
 *
 * @since 4.0.0
 */
public interface BukkitBackend {

    /**
     * Register a fully-built Imperat command with the host platform.
     * Called from {@code BukkitImperat#registerSimpleCommand} after the core
     * has populated its internal command tree.
     */
    void registerCommand(@NotNull Command<BukkitCommandSource> command);

    /**
     * Wrap a platform-supplied sender object into a {@link BukkitCommandSource}.
     * Modern backend may receive a Paper {@code CommandSourceStack}; legacy
     * backend always receives a {@code CommandSender}.
     */
    @NotNull BukkitCommandSource wrapSender(@NotNull Object sender);

    /**
     * Apply argument-type defaults to the supplied config. Called once during
     * {@code BukkitImperat} construction after the backend is selected — modern
     * registers Paper-native types with client-side suggestions, legacy registers
     * the existing name-based bukkit types.
     */
    void applyArgumentTypeDefaults(@NotNull ImperatConfig<BukkitCommandSource> config);

    /**
     * Backend-specific shutdown hook — closes any resources the backend owns.
     */
    void shutdown();

    /**
     * Adventure provider in use by this backend (forwarded from the builder).
     */
    @NotNull AdventureProvider<CommandSender> adventureProvider();
}
