package studio.mevera.imperat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.brigadier.BukkitBrigadierManager;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * Main Imperat implementation for Bukkit/Spigot/Paper servers.
 * This class serves as the primary entry point for integrating the Imperat command framework
 * with Bukkit-based server platforms, providing comprehensive command management capabilities.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Full integration with Bukkit's command system</li>
 *   <li>Adventure API support for rich text messaging</li>
 *   <li>Brigadier integration for Paper servers (optional)</li>
 *   <li>Built-in parameter types for Bukkit objects (Players, Locations, etc.)</li>
 *   <li>Entity selector support (@p, @a, @e, @r)</li>
 *   <li>Automatic command registration and cleanup</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     private BukkitImperat imperat;
 *
 *     @Override
 *     public void onEnable() {
 *         imperat = BukkitImperat.builder(this)
 *             .applyBrigadier(true)  // Enable for Paper
 *             .build();
 *
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
 * @see BukkitSource
 */
public final class BukkitImperat extends BaseImperat<BukkitSource> {

    private final Plugin plugin;
    private final boolean paperPlugin;
    private final AdventureProvider<CommandSender> adventureProvider;
    private BukkitBrigadierManager brigadierManager;
    private Map<String, org.bukkit.command.Command> bukkitCommands = new HashMap<>();

    @SuppressWarnings("unchecked") BukkitImperat(
            Plugin plugin,
            AdventureProvider<CommandSender> adventureProvider,
            boolean supportBrigadier,
            ImperatConfig<BukkitSource> config
    ) {
        super(config);
        this.plugin = plugin;
        this.paperPlugin = isPaperPlugin(plugin);
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

        if (supportBrigadier) {
            applyBrigadier();
        } else {
            applyAsyncTabListener();
        }
    }

    /**
     * Creates a new configuration builder for BukkitImperat.
     * This is the recommended way to create and configure a BukkitImperat instance.
     *
     * @param plugin the plugin instance that will own this Imperat instance
     * @return a new BukkitConfigBuilder for further configuration
     */
    public static BukkitConfigBuilder builder(Plugin plugin) {
        return new BukkitConfigBuilder(plugin);
    }

    @Override
    public BukkitSource createDummySender() {
        return new BukkitSource(Bukkit.getConsoleSender(), adventureProvider);
    }

    /**
     * Wraps the sender into a built-in command-sender valueType
     *
     * @param sender the sender's actual value
     * @return the wrapped command-sender valueType
     */
    @Override
    public BukkitSource wrapSender(Object sender) {
        return new BukkitSource((CommandSender) sender, adventureProvider);
    }

    /**
     * @return the platform of the module
     */
    @Override
    public Plugin getPlatform() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        this.adventureProvider.close();
        Bukkit.getPluginManager().disablePlugin(plugin);
    }

    /**
     * Registering a command into the dispatcher
     *
     * @param command the command to register
     */
    @Override
    public void registerSimpleCommand(Command<BukkitSource> command) {
        super.registerSimpleCommand(command);

        //let's make a safety check for the plugin.yml
        if (!paperPlugin && plugin instanceof JavaPlugin javaPlugin) {
            var existingPluginYamlCmd = javaPlugin.getCommand(command.name().toLowerCase());
            if (existingPluginYamlCmd != null) {
                throw new IllegalArgumentException("Command with name '" + command.name() + "' already exists in plugin.yml!");
            }
        }

        var internalCmd = new InternalBukkitCommand(this, command);

        BukkitUtil.COMMAND_MAP.register(this.plugin.getName(), internalCmd);

        if (brigadierManager != null) {
            brigadierManager.registerBukkitCommand(internalCmd, command, config.getPermissionChecker());
        }
    }

    /**
     * Unregisters a command from the internal registry
     *
     * @param name the name of the command to unregister
     */
    @Override
    public void unregisterCommand(String name) {
        Command<BukkitSource> imperatCmd = getCommand(name);
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
        //BukkitUtil.COMMAND_MAP.clearCommands();
    }


    private void applyBrigadier() {
        if (Version.isOrOver(1, 13, 0)) {
            brigadierManager = BukkitBrigadierManager.load(this);
        }
    }

    private void applyAsyncTabListener() {
        if (Version.SUPPORTS_PAPER_ASYNC_TAB_COMPLETION) {
            plugin.getServer().getPluginManager().registerEvents(new AsyncTabListener(this), plugin);
        }
    }

    private boolean isPaperPlugin(Plugin plugin) {
        if (!Version.IS_PAPER || Version.isOrBelow(1, 13, 0)) {
            return false;
        }

        try {
            URI uri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();

            try (JarFile jar = new JarFile(new File(uri))) {
                return jar.getEntry("paper-plugin.yml") != null;
            }
        } catch (IOException | URISyntaxException e) {
            config().getThrowablePrinter().print(e);
            return false;
        }
    }

}
