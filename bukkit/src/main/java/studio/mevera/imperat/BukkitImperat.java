package studio.mevera.imperat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.BukkitBackend;
import studio.mevera.imperat.backend.capability.BukkitCapability;
import studio.mevera.imperat.backend.capability.CapabilityResolver;
import studio.mevera.imperat.backend.capability.RegistrationCapability;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Main Imperat implementation for Bukkit/Spigot/Paper servers.
 * This class serves as the primary entry point for integrating the Imperat command framework
 * with Bukkit-based server platforms, providing comprehensive command management capabilities.
 *
 * <p>Backend selection is automatic — at construction time the runtime classpath
 * is probed; if Paper's modern Brigadier API ({@code io.papermc.paper.command.brigadier.Commands})
 * <i>and</i> the lifecycle event types are present, the modern backend is selected
 * and Brigadier integration is wired transparently. Otherwise the legacy backend
 * registers commands via Bukkit's command-map reflection with no Brigadier on the
 * legacy path.</p>
 *
 * <p>Plugin authors choose nothing — both classpaths produce the same
 * {@code BukkitImperat} entry-point.</p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     private BukkitImperat imperat;
 *
 *     @Override
 *     public void onEnable() {
 *         imperat = BukkitImperat.builder(this).build();
 *         imperat.registerCommand(MyCommand.class);
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         if (imperat != null) {
 *             imperat.shutdownPlatform();
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see BukkitConfigBuilder
 * @see BukkitCommandSource
 */
public final class BukkitImperat extends BaseImperat<BukkitCommandSource> {

    private final Plugin plugin;
    private final AdventureProvider<CommandSender> adventureProvider;
    private final RegistrationCapability backend;
    private Map<String, org.bukkit.command.Command> bukkitCommands = new HashMap<>();

    @SuppressWarnings("unchecked") BukkitImperat(
            Plugin plugin,
            AdventureProvider<CommandSender> adventureProvider,
            ImperatConfig<BukkitCommandSource> config,
            boolean rewriteUnknownCommandMessage
    ) {
        super(config);
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;

        ImperatDebugger.setLogger(plugin.getLogger());

        try {
            if (BukkitUtil.KNOWN_COMMANDS != null) {
                this.bukkitCommands = (Map<String, org.bukkit.command.Command>)
                                              BukkitUtil.KNOWN_COMMANDS.get(BukkitUtil.COMMAND_MAP);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // Capability resolver picks the right registration backend based on
        // runtime class probes (modern Paper > legacy Paper Brigadier event >
        // Commodore > plain CommandMap). Impl class is loaded reflectively
        // — probed-but-unselected impls never hit the classloader.
        this.backend = CapabilityResolver.resolve(plugin);
        this.backend.initialize(plugin, this, adventureProvider);

        // Argument-type defaults are registered LATE — backend-specific.
        backend.applyArgumentTypeDefaults(config);

        // Permission-denied rewrite via UnknownCommandEvent is independent
        // of registration backend — wired here so all Bukkit-flavoured
        // backends benefit when the option is enabled.
        if (rewriteUnknownCommandMessage) {
            registerUnknownCommandListener();
        }

        // AsyncTabCompleteEvent listener — registered ONLY on the plain
        // CommandMap backend, where Brigadier ISN'T producing native
        // suggestions and the listener's flat Imperat-tree output is the
        // only path to tab completion. On Brigadier-capable backends
        // (modern Paper, legacy Paper, Commodore) this event also fires
        // AFTER Brigadier populates completions; the listener's
        // {@code event.setCompletions(...)} + {@code setHandled(true)}
        // would overwrite native selector menus, filter keys, and other
        // rich client-side autocomplete with our flat list. Gating on
        // capability keeps the two paths mutually exclusive.
        if (Version.SUPPORTS_PAPER_ASYNC_TAB_COMPLETION
                    && backend.kind() == BukkitCapability.PLAIN_COMMAND_MAP) {
            plugin.getServer().getPluginManager().registerEvents(new AsyncTabListener(this), plugin);
        }

        registerAutoCleanupListener();
    }

    /**
     * Auto-unregisters Imperat commands when the owning plugin is disabled.
     * Bukkit's {@link org.bukkit.command.CommandMap} doesn't unregister
     * plugin-scoped entries on plugin disable — without this listener,
     * a hot-reload or {@code /reload} leaves orphan command entries that
     * either ghost on the next enable or trip the registration's
     * "command already registered" path. Mirrors the standard
     * {@code PluginDisableEvent} cleanup pattern other command frameworks
     * use.
     *
     * <p>Calls {@link #unregisterAllCommands()} + {@link #shutdownPlatform()}
     * — both are safe to call again from the user's {@code onDisable}
     * (registry is empty after the first run; backend's
     * {@code adventureProvider.close()} is null-guarded).</p>
     */
    private void registerAutoCleanupListener() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent event) {
                if (event.getPlugin() == plugin) {
                    unregisterAllCommands();
                    shutdownPlatform();
                }
            }
        }, plugin);
    }

    private static String stripLabel(@NotNull String commandLine) {
        String trimmed = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        int space = trimmed.indexOf(' ');
        String label = space >= 0 ? trimmed.substring(0, space) : trimmed;
        return label.isEmpty() ? null : label.toLowerCase();
    }

    private void registerUnknownCommandListener() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onUnknown(UnknownCommandEvent event) {
                String line = event.getCommandLine();
                if (line == null || line.isEmpty()) {
                    return;
                }
                String label = stripLabel(line);
                if (label == null) {
                    return;
                }
                Command<BukkitCommandSource> imperatCommand = getCommand(label);
                if (imperatCommand == null) {
                    return;
                }
                event.message(null);
                execute(wrapSender(event.getSender()), line);
            }
        }, plugin);
    }

    /**
     * Creates a new configuration builder for BukkitImperat.
     * Brigadier integration is auto-wired based on the runtime — no toggle.
     *
     * @param plugin the plugin instance that will own this Imperat instance
     * @return a new BukkitConfigBuilder for further configuration
     */
    public static BukkitConfigBuilder builder(Plugin plugin) {
        return new BukkitConfigBuilder(plugin);
    }

    /** @return the active backend (modern Paper, legacy Paper, Commodore, or plain CommandMap). */
    public BukkitBackend backend() {
        return backend;
    }

    /** @return the capability that satisfied the runtime probe — for diagnostics. */
    public BukkitCapability capability() {
        return backend.kind();
    }

    @Override
    public BukkitCommandSource createDummySender() {
        return new BukkitCommandSource(Bukkit.getConsoleSender(), adventureProvider);
    }

    /**
     * Wraps the sender into the platform-aware command-source type. Modern
     * backend may receive a Paper {@code CommandSourceStack}; legacy backend
     * always receives a {@code CommandSender}. Backend-specific logic is
     * delegated to the active {@link BukkitBackend}.
     *
     * @param sender the platform sender (CommandSender or CommandSourceStack)
     * @return the wrapped command source
     */
    @Override
    public BukkitCommandSource wrapSender(Object sender) {
        return backend.wrapSender(sender);
    }

    /** @return the platform plugin instance. */
    @Override
    public Plugin getPlatform() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        backend.shutdown();
    }

    /**
     * Registering a command into the dispatcher
     *
     * @param command the command to register
     */
    @Override
    public void registerSimpleCommand(Command<BukkitCommandSource> command) {
        super.registerSimpleCommand(command);
        backend.registerCommand(command);
    }

    public void updateCommand(Command<BukkitCommandSource> command) {
        registerSimpleCommand(command);
    }

    /**
     * Unregisters a command from the internal registry
     *
     * @param name the name of the command to unregister
     */
    @Override
    public void unregisterCommand(String name) {
        Command<BukkitCommandSource> imperatCmd = getCommand(name);
        super.unregisterCommand(name);

        if (imperatCmd == null) {
            return;
        }
        for (var entry : new HashSet<>(bukkitCommands.entrySet())) {
            var originalKey = entry.getKey();
            var key = StringUtils.stripNamespace(originalKey);

            if (imperatCmd.hasName(key)) {
                bukkitCommands.remove(originalKey);
            }
        }
        try {
            if (BukkitUtil.KNOWN_COMMANDS != null) {
                BukkitUtil.KNOWN_COMMANDS.set(BukkitUtil.COMMAND_MAP, bukkitCommands);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregisters all commands from the internal registry
     */
    @Override
    public void unregisterAllCommands() {
        super.unregisterAllCommands();
        if (BukkitUtil.KNOWN_COMMANDS != null) {
            bukkitCommands.clear();
            try {
                BukkitUtil.KNOWN_COMMANDS.set(BukkitUtil.COMMAND_MAP, bukkitCommands);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
