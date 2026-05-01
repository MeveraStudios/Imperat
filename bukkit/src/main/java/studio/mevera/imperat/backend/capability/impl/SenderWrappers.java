package studio.mevera.imperat.backend.capability.impl;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.adventure.AdventureProvider;

/**
 * Package-private utility consolidating the cross-backend pieces of
 * {@link studio.mevera.imperat.backend.capability.RegistrationCapability#wrapSender(Object)}
 * implementations. Stays free of Paper-only types so it loads safely
 * under every active backend (modern Paper, legacy Paper, Commodore,
 * plain Spigot) — the Paper-flavoured branches that need
 * {@code CommandSourceStack} / {@code BukkitBrigadierCommandSource}
 * remain inlined in the Registration class for that backend (those
 * classes are reflectively loaded only when their capability is
 * detected, so referencing the missing Paper class there is safe).
 *
 * @since 4.0.0
 */
final class SenderWrappers {

    private SenderWrappers() {
    }

    /**
     * Plain {@link CommandSender} wrapping — shared by every backend that
     * receives a stock Bukkit sender (plain CommandMap, Commodore after
     * NMS unwrap, Paper backends as a fallback when the type-specific
     * branch falls through).
     */
    static @NotNull BukkitCommandSource plain(
            @NotNull CommandSender sender,
            @NotNull AdventureProvider<CommandSender> adventureProvider
    ) {
        return new BukkitCommandSource(sender, adventureProvider);
    }

    /**
     * Standard "expected X or Y" rejection so every backend's error
     * message follows one format.
     */
    static @NotNull IllegalArgumentException reject(@NotNull Object sender, @NotNull String expected) {
        return new IllegalArgumentException(
                "Cannot wrap sender of type " + sender.getClass().getName()
                        + " — expected " + expected);
    }
}
