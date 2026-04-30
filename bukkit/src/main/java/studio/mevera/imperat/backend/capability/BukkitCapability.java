package studio.mevera.imperat.backend.capability;

import org.jetbrains.annotations.NotNull;

/**
 * Runtime-detectable platform capabilities the bukkit module discriminates
 * between. Each capability documents its supported version range and
 * platform; {@link CapabilityResolver} probes them in priority order at
 * {@code BukkitImperat} construction.
 *
 * <p><b>Detection contract</b> — {@link #capable(BukkitClassProbe)} must
 * be reflection-only. No imports of probed classes outside the impl
 * packages. Probes use the plugin's classloader without triggering
 * class-initialization.</p>
 *
 * @since 4.0.0
 */
public enum BukkitCapability {

    /**
     * Brigadier umbrella — true iff the {@code com.mojang.brigadier}
     * package is reachable. Required prerequisite for any of the
     * Brigadier-driven capabilities below.
     *
     * <ul>
     *   <li><b>Range:</b> 1.13+</li>
     *   <li><b>Platform:</b> Spigot + Paper</li>
     * </ul>
     */
    BRIGADIER {
        @Override
        public boolean capable(@NotNull BukkitClassProbe probe) {
            return probe.exists("com.mojang.brigadier.tree.CommandNode");
        }
    },

    /**
     * Modern Paper's stable Brigadier API + lifecycle event registrar.
     * Backed by {@code io.papermc.paper.command.brigadier.Commands} and
     * {@code LifecycleEvents.COMMANDS}. No NMS, no reflection.
     *
     * <ul>
     *   <li><b>Range:</b> Paper 1.21.4+</li>
     *   <li><b>Platform:</b> Paper only</li>
     * </ul>
     */
    MODERN_NATIVE_BRIGADIER {
        @Override
        public boolean capable(@NotNull BukkitClassProbe probe) {
            return probe.exists("io.papermc.paper.command.brigadier.Commands")
                           && probe.exists("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents");
        }
    },

    /**
     * Legacy Paper Brigadier hook — the
     * {@code com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent}
     * path Paper exposed before the modern Brigadier API stabilised.
     * Mutually exclusive with {@link #MODERN_NATIVE_BRIGADIER}: the
     * resolver picks Modern when both are capable.
     *
     * <ul>
     *   <li><b>Range:</b> Paper 1.13 – 1.21.3</li>
     *   <li><b>Platform:</b> Paper only</li>
     * </ul>
     */
    PAPER_LEGACY_BRIGADIER {
        @Override
        public boolean capable(@NotNull BukkitClassProbe probe) {
            return probe.exists("com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent")
                           && !MODERN_NATIVE_BRIGADIER.capable(probe);
        }
    },

    /**
     * Commodore-driven Brigadier registration via NMS reflection. Used
     * on Spigot + pre-Brigadier-event Paper builds. Excluded on 1.19+
     * (Warden class probe) because Commodore's reflection breaks past
     * that point — those servers fall through to PlainCommandMap.
     *
     * <ul>
     *   <li><b>Range:</b> Spigot/Paper 1.13 – 1.18.x</li>
     *   <li><b>Platform:</b> Spigot + Paper (when Paper-Brigadier event absent)</li>
     * </ul>
     */
    COMMODORE_BRIGADIER {
        @Override
        public boolean capable(@NotNull BukkitClassProbe probe) {
            return BRIGADIER.capable(probe)
                           && !MODERN_NATIVE_BRIGADIER.capable(probe)
                           && !PAPER_LEGACY_BRIGADIER.capable(probe)
                           && !probe.exists("org.bukkit.entity.Warden");
        }
    },

    /**
     * The universal fallback — register commands on Bukkit's
     * {@code SimpleCommandMap} via reflection. No Brigadier integration:
     * tab completion goes through {@code TabCompleter} on the registered
     * Bukkit command, calling Imperat's auto-completer synchronously.
     *
     * <ul>
     *   <li><b>Range:</b> 1.8+</li>
     *   <li><b>Platform:</b> Spigot + Paper (always)</li>
     * </ul>
     */
    PLAIN_COMMAND_MAP {
        @Override
        public boolean capable(@NotNull BukkitClassProbe probe) {
            return true;
        }
    };

    public abstract boolean capable(@NotNull BukkitClassProbe probe);
}
