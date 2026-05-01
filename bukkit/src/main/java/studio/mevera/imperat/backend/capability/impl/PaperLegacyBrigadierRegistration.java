package studio.mevera.imperat.backend.capability.impl;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
import studio.mevera.imperat.backend.modern.BukkitBrigadierManager;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.OfflinePlayerArgument;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.TargetSelectorArgument;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@link BukkitCapability#PAPER_LEGACY_BRIGADIER} registration impl —
 * Paper 1.13 to 1.21.3 (pre-modern-Brigadier-API). Registers commands
 * via {@code CommandMap} like the plain backend, then listens to
 * {@link CommandRegisteredEvent} to swap the literal node with the
 * Brigadier-built tree so the client gets native tab completion.
 *
 * <ul>
 *   <li><b>Range:</b> Paper 1.13 – 1.21.3</li>
 *   <li><b>Platform:</b> Paper only (event class is Paper-specific)</li>
 * </ul>
 *
 * @since 4.0.0
 */
@SuppressWarnings("all")
public final class PaperLegacyBrigadierRegistration implements RegistrationCapability {

    /**
     * Pending Brigadier nodes keyed by lowercase command name. Filled by
     * {@link #registerCommand} and consumed by the
     * {@link CommandRegisteredEvent} listener as Paper announces each
     * command's Brigadier registration.
     */
    private final Map<String, LiteralCommandNode<BukkitBrigadierCommandSource>> pendingNodes =
            new HashMap<>();
    private BukkitImperat owner;
    private Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;
    private BukkitBrigadierManager brigadierManager;

    public PaperLegacyBrigadierRegistration() {
    }

    @Override
    public void initialize(@NotNull Plugin plugin,
            @NotNull BukkitImperat imperat,
            @NotNull AdventureProvider<CommandSender> adventureProvider) {
        this.owner = imperat;
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        this.brigadierManager = new BukkitBrigadierManager(imperat);

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onCommandRegistered(CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
                String label = event.getCommandLabel().toLowerCase(Locale.ROOT);
                LiteralCommandNode<BukkitBrigadierCommandSource> imperatNode = pendingNodes.remove(label);
                if (imperatNode != null) {
                    event.setLiteral(imperatNode);
                }
            }
        }, plugin);
    }

    @Override
    public void registerCommand(@NotNull Command<BukkitCommandSource> command) {
        // Phase 1: register via CommandMap so Bukkit's dispatcher knows
        // the command exists (and can route execute() / fall back if the
        // Brigadier hookup misses for any reason).
        InternalBukkitCommand internalCmd = new InternalBukkitCommand(owner, command);
        BukkitUtil.COMMAND_MAP.register(plugin.getName(), internalCmd);

        // Phase 2: stash the Brigadier-built node — the
        // CommandRegisteredEvent listener swaps it in when Paper fires
        // the event for this command.
        LiteralCommandNode<BukkitBrigadierCommandSource> node =
                brigadierManager.parseCommandIntoNode(command);
        pendingNodes.put(command.getName().toLowerCase(Locale.ROOT), node);
        for (String alias : command.aliases()) {
            pendingNodes.put(alias.toLowerCase(Locale.ROOT), node);
        }
    }

    @Override
    public @NotNull BukkitCommandSource wrapSender(@NotNull Object sender) {
        if (sender instanceof BukkitBrigadierCommandSource brig) {
            return SenderWrappers.plain(brig.getBukkitSender(), adventureProvider);
        }
        if (sender instanceof CommandSender plain) {
            return SenderWrappers.plain(plain, adventureProvider);
        }
        throw SenderWrappers.reject(sender, "BukkitBrigadierCommandSource or CommandSender");
    }

    @Override
    public void applyArgumentTypeDefaults(@NotNull ImperatConfig<BukkitCommandSource> config) {
        // Legacy Paper Brigadier has no stable native Java→Brigadier
        // mapping API equivalent to modern Paper's ArgumentTypes. Fall
        // back to the name-based bukkit types — tab suggestions still
        // arrive via Imperat's customSuggestions over Brigadier.
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
        return BukkitCapability.PAPER_LEGACY_BRIGADIER;
    }

}
