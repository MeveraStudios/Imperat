package studio.mevera.imperat.backend.capability.impl;

import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.InternalBukkitCommand;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.capability.BukkitCapability;
import studio.mevera.imperat.backend.capability.RegistrationCapability;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.commodore.Commodore;
import studio.mevera.imperat.commodore.CommodoreProvider;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.OfflinePlayerArgument;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.TargetSelectorArgument;

/**
 * {@link BukkitCapability#COMMODORE_BRIGADIER} registration impl —
 * Spigot / pre-Brigadier-event Paper (1.13 to 1.18.x). Registers the
 * command via {@code CommandMap} and pushes a Brigadier tree to clients
 * through Commodore's NMS reflection bridge.
 *
 * <ul>
 *   <li><b>Range:</b> Spigot/Paper 1.13 – 1.18.x</li>
 *   <li><b>Platform:</b> Spigot + Paper (when modern + legacy Paper paths
 *       are absent)</li>
 * </ul>
 *
 * <p>If Commodore's reflective hookup fails (NMS package moved, server
 * fork rearranged internals), {@link CommodoreProvider#getCommodore}
 * returns {@code null} and the registration falls through to plain
 * CommandMap behaviour — server still works, only client-side tab
 * completion drops to legacy mode for that command.</p>
 *
 * @since 4.0.0
 */
public final class CommodoreRegistration implements RegistrationCapability {

    private BukkitImperat owner;
    private Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;
    private @Nullable Commodore<org.bukkit.command.Command> commodore;
    private CommodoreBrigadierManager brigadierManager;

    public CommodoreRegistration() {
    }

    @Override
    public void initialize(@NotNull Plugin plugin,
            @NotNull BukkitImperat imperat,
            @NotNull AdventureProvider<CommandSender> adventureProvider) {
        this.owner = imperat;
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        this.commodore = CommodoreProvider.getCommodore(imperat);
        this.brigadierManager = new CommodoreBrigadierManager(imperat);
    }

    @Override
    public void registerCommand(@NotNull Command<BukkitCommandSource> command) {
        InternalBukkitCommand internalCmd = new InternalBukkitCommand(owner, command);
        BukkitUtil.COMMAND_MAP.register(plugin.getName(), internalCmd);

        if (commodore == null) {
            return; // Commodore unavailable — plain registration is sufficient.
        }
        LiteralCommandNode<Object> node = brigadierManager.parseCommandIntoNode(command);
        commodore.register(internalCmd, node);
    }

    @Override
    public @NotNull BukkitCommandSource wrapSender(@NotNull Object sender) {
        if (sender instanceof CommandSender plain) {
            return new BukkitCommandSource(plain, adventureProvider);
        }
        // Commodore hands NMS sources to the Brigadier predicate — defer
        // to its wrapper to convert back to Bukkit's CommandSender.
        if (commodore != null) {
            CommandSender wrapped = commodore.wrapNMSCommandSource(sender);
            if (wrapped != null) {
                return new BukkitCommandSource(wrapped, adventureProvider);
            }
        }
        throw new IllegalArgumentException(
                "Cannot wrap sender of type " + sender.getClass().getName()
                        + " — expected CommandSender or NMS source");
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
        return BukkitCapability.COMMODORE_BRIGADIER;
    }

    /** Brigadier tree builder for the Commodore path — uses raw Object as native source. */
    private final class CommodoreBrigadierManager extends BaseBrigadierManager<BukkitCommandSource> {

        private CommodoreBrigadierManager(@NotNull BukkitImperat dispatcher) {
            super(dispatcher);
        }

        @Override
        public BukkitCommandSource wrapCommandSource(Object commandSource) {
            return CommodoreRegistration.this.wrapSender(commandSource);
        }

        @Override
        public @NotNull com.mojang.brigadier.arguments.ArgumentType<?> getArgumentType(
                @NotNull Argument<BukkitCommandSource> imperatArgument) {
            return getStringArgType(imperatArgument);
        }
    }
}
