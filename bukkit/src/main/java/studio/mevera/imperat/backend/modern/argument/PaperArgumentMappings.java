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
import studio.mevera.imperat.backend.modern.type.PaperLocationArgument;
import studio.mevera.imperat.backend.modern.type.PaperOfflinePlayerArgument;
import studio.mevera.imperat.backend.modern.type.PaperPlayerArgument;

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
        config.registerArgType(Player.class, new PaperPlayerArgument());

        // OfflinePlayer kept on the legacy name-based path (Paper's
        // playerProfiles selector returns PlayerProfile, not OfflinePlayer,
        // and most plugin code wants the bukkit OfflinePlayer view).
        config.registerArgType(OfflinePlayer.class, new PaperOfflinePlayerArgument());

        // Location → legacy multi-token name-based parser (kept for
        // callers using the bukkit-style "world;x;y;z" form). Plugin
        // authors who want selector-style positions can register the
        // FinePosition mapping themselves at use-site.
        config.registerArgType(Location.class, new PaperLocationArgument());

        // Identity-resolved native types (parsed form == friendly type).
        config.registerArgType(World.class,
                new PaperBukkitArgumentType<>(World.class, PaperArgumentType.identity(ArgumentTypes.world())));

        config.registerArgType(GameMode.class,
                new PaperBukkitArgumentType<>(GameMode.class, PaperArgumentType.identity(ArgumentTypes.gameMode())));

        config.registerArgType(ItemStack.class,
                new PaperBukkitArgumentType<>(ItemStack.class, PaperArgumentType.identity(ArgumentTypes.itemStack())));

        config.registerArgType(NamespacedKey.class,
                new PaperBukkitArgumentType<>(NamespacedKey.class, PaperArgumentType.identity(ArgumentTypes.namespacedKey())));

        // Override the core's UUID with Paper's native UUID arg-type.
        config.registerArgType(UUID.class,
                new PaperBukkitArgumentType<>(UUID.class, PaperArgumentType.identity(ArgumentTypes.uuid())));
    }
}
