package studio.mevera.imperat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.backend.BukkitBackend;
import studio.mevera.imperat.backend.legacy.LegacyBackend;
import studio.mevera.imperat.backend.modern.ModernPaperBackend;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.StringUtils;
import studio.mevera.imperat.util.reflection.Reflections;

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
    private final BukkitBackend backend;
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

        // Probe the runtime classpath for modern Paper Brigadier support.
        // Both gates must be present — Commands alone isn't enough; we also
        // need the lifecycle-event API to capture the registrar safely.
        boolean modernPaper =
                Reflections.findClass("io.papermc.paper.command.brigadier.Commands")
                        && Reflections.findClass("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents");

        if (modernPaper) {
            this.backend = new ModernPaperBackend(this, plugin, adventureProvider, rewriteUnknownCommandMessage);
        } else {
            this.backend = new LegacyBackend(this, plugin, adventureProvider);
        }

        // Argument-type defaults are registered LATE — backend-specific.
        backend.applyArgumentTypeDefaults(config);
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

    /** @return the active backend (modern Paper or legacy). */
    public BukkitBackend backend() {
        return backend;
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
