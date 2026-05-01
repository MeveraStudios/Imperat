package studio.mevera.imperat.backend.modern.argument;

import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.backend.modern.type.PaperTargetSelectorArgument;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.OfflinePlayerArgument;
import studio.mevera.imperat.type.PlayerArgument;

import java.util.UUID;

/**
 * Default Java-type → Paper Brigadier {@code ArgumentType} mappings
 *
 * <p>Each mapping wraps a {@link PaperArgumentType} (native Paper
 * Brigadier {@code ArgumentType} + resolver) into a
 * {@link PaperBukkitArgumentType} so the Mojang client gets <b>native
 * client-side suggestions</b> for these types — no server-side
 * {@code customSuggestions} dance, no flaky tab completion. The resolver
 * receives the executing {@code CommandSourceStack} at parse time so
 * selectors (e.g. {@code @p}, {@code @a}) can resolve against the source.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperArgumentMappings {

    private PaperArgumentMappings() {
    }

    public static void applyDefaults(ImperatConfig<BukkitCommandSource> config) {
        // Player → name-based Imperat-side argument (mirror of legacy
        // bukkit module). Suggestion provider returns online player names
        // via Imperat's customSuggestions path.
        config.registerArgType(Player.class, new PlayerArgument());

        // OfflinePlayer kept on the legacy name-based path (Paper's
        // playerProfiles selector returns PlayerProfile, not OfflinePlayer,
        // and most plugin code wants the bukkit OfflinePlayer view).
        config.registerArgType(OfflinePlayer.class, new OfflinePlayerArgument());

        // Location → legacy multi-token name-based parser (kept for
        // callers using the bukkit-style "world;x;y;z" form). Plugin
        // authors who want selector-style positions can register the
        // FinePosition mapping themselves at use-site.
        config.registerArgType(Location.class, new LocationArgument());

        // TargetSelector → Paper-native-aware variant. Server-side parsing
        // stays in the legacy TargetSelectorArgument; client gets
        // ArgumentTypes.entities() for native @e[...] coloring +
        // autocomplete via the PaperNativeAware bridge.
        config.registerArgType(TargetSelector.class, new PaperTargetSelectorArgument());

        // Identity-resolved native types — eagerly call ArgumentTypes.X()
        // which can throw under test mocks (MockBukkit's
        // VanillaArgumentProviderMock raises a TestAbortedException-style
        // UnimplementedOperationException). Each registration is isolated
        // so a single mock-unsupported type does not skip the entire test
        // (TestAbortedException would otherwise propagate up through
        // `BukkitImperat`'s ctor and JUnit would mark the @BeforeEach as
        // aborted, silently skipping every test in the class).
        registerNative(config, World.class, ArgumentTypes::world);
        registerNative(config, GameMode.class, ArgumentTypes::gameMode);
        registerNative(config, ItemStack.class, ArgumentTypes::itemStack);
        registerNative(config, NamespacedKey.class, ArgumentTypes::namespacedKey);
        registerNative(config, UUID.class, ArgumentTypes::uuid);
    }

    private static <T> void registerNative(
            ImperatConfig<BukkitCommandSource> config,
            Class<T> type,
            java.util.function.Supplier<com.mojang.brigadier.arguments.ArgumentType<T>> factory
    ) {
        com.mojang.brigadier.arguments.ArgumentType<T> nativeType;
        try {
            nativeType = factory.get();
        } catch (Throwable ex) {
            // Mock environments (MockBukkit) raise here for types they
            // haven't implemented. Skip the mapping rather than aborting
            // the surrounding test.
            return;
        }
        config.registerArgType(type, new PaperBukkitArgumentType<>(type, PaperArgumentType.identity(nativeType)));
    }
}
