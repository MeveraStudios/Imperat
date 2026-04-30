package studio.mevera.imperat.backend.capability;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.Map;

/**
 * Strategy-chain resolver for {@link RegistrationCapability}. Probes the
 * runtime in priority order:
 *
 * <ol>
 *   <li>{@link BukkitCapability#MODERN_NATIVE_BRIGADIER}</li>
 *   <li>{@link BukkitCapability#PAPER_LEGACY_BRIGADIER}</li>
 *   <li>{@link BukkitCapability#COMMODORE_BRIGADIER}</li>
 *   <li>{@link BukkitCapability#PLAIN_COMMAND_MAP} (always capable)</li>
 * </ol>
 *
 * <p>Impls are loaded reflectively by FQN so a missing capability
 * (e.g. paper-api absent on legacy Spigot) never triggers a class-load
 * on its impl class. This keeps the lower-bound classpath requirement
 * down to plain Bukkit.</p>
 *
 * <p><b>Failure policy:</b> if the chosen impl's instantiation throws
 * (NMS reflection failed, Commodore broken on this version, etc.) the
 * exception PROPAGATES — the plugin's {@code onEnable} fails loud
 * rather than silently degrading. Server admins see the actual cause.</p>
 *
 * @since 4.0.0
 */
public final class CapabilityResolver {

    /** FQN of each capability's impl — keyed reflectively. */
    private static final Map<BukkitCapability, String> IMPL_FQN = Map.of(
            BukkitCapability.MODERN_NATIVE_BRIGADIER,
            "studio.mevera.imperat.backend.capability.impl.ModernPaperRegistration",
            BukkitCapability.PAPER_LEGACY_BRIGADIER,
            "studio.mevera.imperat.backend.capability.impl.PaperLegacyBrigadierRegistration",
            BukkitCapability.COMMODORE_BRIGADIER,
            "studio.mevera.imperat.backend.capability.impl.CommodoreRegistration",
            BukkitCapability.PLAIN_COMMAND_MAP,
            "studio.mevera.imperat.backend.capability.impl.PlainCommandMapRegistration"
    );

    private static final BukkitCapability[] PRIORITY_CHAIN = {
            BukkitCapability.MODERN_NATIVE_BRIGADIER,
            BukkitCapability.PAPER_LEGACY_BRIGADIER,
            BukkitCapability.COMMODORE_BRIGADIER,
            BukkitCapability.PLAIN_COMMAND_MAP
    };

    private CapabilityResolver() {
    }

    public static @NotNull RegistrationCapability resolve(@NotNull Plugin plugin) {
        BukkitClassProbe probe = BukkitClassProbe.forPlugin(plugin);
        for (BukkitCapability capability : PRIORITY_CHAIN) {
            if (capability.capable(probe)) {
                ImperatDebugger.debug("[imperat] resolved registration capability: " + capability);
                return loadImpl(capability, probe.classLoader());
            }
        }
        // PLAIN_COMMAND_MAP is always capable, so this is unreachable —
        // guard for safety against future capability-table edits.
        throw new IllegalStateException("No bukkit capability resolved — "
                                                + "PLAIN_COMMAND_MAP fallback is supposed to be always-capable");
    }

    private static @NotNull RegistrationCapability loadImpl(
            @NotNull BukkitCapability capability,
            @NotNull ClassLoader classLoader
    ) {
        String fqn = IMPL_FQN.get(capability);
        try {
            Class<?> implClass = Class.forName(fqn, true, classLoader);
            return (RegistrationCapability) implClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Failed to instantiate registration impl for capability "
                            + capability + " (" + fqn + "): " + ex.getMessage(),
                    ex);
        }
    }
}
