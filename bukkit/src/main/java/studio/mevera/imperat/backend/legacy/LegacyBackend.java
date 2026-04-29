package studio.mevera.imperat.backend.legacy;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.AsyncTabListener;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.InternalBukkitCommand;
import studio.mevera.imperat.Version;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.BukkitBackend;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.OfflinePlayerArgument;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.TargetSelectorArgument;

/**
 * Legacy backend for Spigot / pre-1.21 Paper. Registers commands via
 * Bukkit's {@link org.bukkit.command.CommandMap} reflection and routes
 * tab completion through {@link InternalBukkitCommand#tabComplete} —
 * no Brigadier integration on this path (the dedicated NMS / Commodore
 * codepaths were dropped in favour of the modern Paper backend's
 * stable Brigadier API).
 *
 * <p>If the runtime additionally exposes Paper's
 * {@code AsyncTabCompleteEvent}, this backend also registers an
 * {@link AsyncTabListener} so async tab completion still works on
 * legacy Paper (1.13–1.20) without Brigadier.</p>
 *
 * @since 4.0.0
 */
public final class LegacyBackend implements BukkitBackend {

    private final BukkitImperat owner;
    private final Plugin plugin;
    private final AdventureProvider<CommandSender> adventureProvider;

    public LegacyBackend(
            @NotNull BukkitImperat owner,
            @NotNull Plugin plugin,
            @NotNull AdventureProvider<CommandSender> adventureProvider
    ) {
        this.owner = owner;
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;

        if (Version.SUPPORTS_PAPER_ASYNC_TAB_COMPLETION) {
            plugin.getServer().getPluginManager().registerEvents(new AsyncTabListener(owner), plugin);
        }
    }

    @Override
    public void registerCommand(@NotNull Command<BukkitCommandSource> command) {
        InternalBukkitCommand internalCmd = new InternalBukkitCommand(owner, command);
        BukkitUtil.COMMAND_MAP.register(plugin.getName(), internalCmd);
    }

    @Override
    public @NotNull BukkitCommandSource wrapSender(@NotNull Object sender) {
        if (sender instanceof CommandSender plain) {
            return new BukkitCommandSource(plain, adventureProvider);
        }
        throw new IllegalArgumentException(
                "Cannot wrap sender of type " + sender.getClass().getName()
                        + " — expected CommandSender (legacy backend)");
    }

    @Override
    public void applyArgumentTypeDefaults(@NotNull ImperatConfig<BukkitCommandSource> config) {
        config.registerArgType(Player.class, new PlayerArgument());
        config.registerArgType(OfflinePlayer.class, new OfflinePlayerArgument());
        config.registerArgType(Location.class, new LocationArgument());
        config.registerArgType(TargetSelector.class, new TargetSelectorArgument());
    }

    @Override
    public void shutdown() {
        adventureProvider.close();
    }

    @Override
    public @NotNull AdventureProvider<CommandSender> adventureProvider() {
        return adventureProvider;
    }
}
