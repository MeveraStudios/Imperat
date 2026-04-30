package studio.mevera.imperat.backend.capability.impl;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.InternalBukkitCommand;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.capability.BukkitCapability;
import studio.mevera.imperat.backend.capability.RegistrationCapability;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.OfflinePlayerArgument;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.TargetSelectorArgument;

/**
 * {@link BukkitCapability#PLAIN_COMMAND_MAP} registration impl — universal
 * fallback. Registers commands on Bukkit's
 * {@link org.bukkit.command.CommandMap} via reflection. No Brigadier
 * integration: tab completion routes through
 * {@link InternalBukkitCommand#tabComplete} into Imperat's auto-completer.
 *
 * <ul>
 *   <li><b>Range:</b> 1.8+</li>
 *   <li><b>Platform:</b> Spigot + Paper (always available)</li>
 * </ul>
 *
 * @since 4.0.0
 */
public final class PlainCommandMapRegistration implements RegistrationCapability {

    private BukkitImperat owner;
    private Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;

    public PlainCommandMapRegistration() {
    }

    @Override
    public void initialize(@NotNull Plugin plugin,
            @NotNull BukkitImperat imperat,
            @NotNull AdventureProvider<CommandSender> adventureProvider) {
        this.owner = imperat;
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
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
                        + " — expected CommandSender (plain command-map backend)");
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
        if (adventureProvider != null) {
            adventureProvider.close();
        }
    }

    @Override
    public @NotNull AdventureProvider<CommandSender> adventureProvider() {
        return adventureProvider;
    }

    @Override
    public @NotNull BukkitCapability kind() {
        return BukkitCapability.PLAIN_COMMAND_MAP;
    }
}
