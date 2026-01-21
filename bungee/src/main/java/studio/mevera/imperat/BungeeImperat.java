package studio.mevera.imperat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.StringUtils;

import java.util.HashSet;

/**
 * Main Imperat implementation for BungeeCord proxy servers.
 * This class serves as the primary entry point for integrating the Imperat command framework
 * with BungeeCord proxy networks, providing cross-server command management capabilities.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Full integration with BungeeCord's command system</li>
 *   <li>Adventure API support for rich text messaging</li>
 *   <li>Built-in parameter types for BungeeCord objects (ProxiedPlayer, ServerInfo)</li>
 *   <li>Cross-server player management commands</li>
 *   <li>Automatic command registration and cleanup</li>
 *   <li>Legacy BaseComponent support for backward compatibility</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * public class MyBungeePlugin extends Plugin {
 *     private BungeeImperat imperat;
 *
 *     @Override
 *     public void onEnable() {
 *         imperat = BungeeImperat.builder(this)
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
 * @author Imperat Framework
 * @see BungeeConfigBuilder
 * @see BungeeSource
 * @since 1.0
 */
public final class BungeeImperat extends BaseImperat<BungeeSource> {

    private final Plugin plugin;
    private final AdventureProvider<CommandSender> adventureProvider;

    /**
     * Package-private constructor used by BungeeConfigBuilder.
     * Use {@link #builder(Plugin)} to create instances.
     *
     * @param plugin            the plugin instance
     * @param adventureProvider the Adventure provider for rich text messaging
     * @param config            the Imperat configuration
     */
    BungeeImperat(
            Plugin plugin,
            @NotNull AdventureProvider<CommandSender> adventureProvider,
            ImperatConfig<BungeeSource> config
    ) {
        super(config);
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        ImperatDebugger.setLogger(plugin.getLogger());
    }

    /**
     * Creates a new configuration builder for BungeeImperat.
     * This is the recommended way to create and configure a BungeeImperat instance.
     *
     * @param plugin the plugin instance that will own this Imperat instance
     * @return a new BungeeConfigBuilder for further configuration
     */
    public static BungeeConfigBuilder builder(Plugin plugin) {
        return new BungeeConfigBuilder(plugin, null);
    }

    @Override
    public void registerCommand(Command<BungeeSource> command) {
        super.registerCommand(command);
        plugin.getProxy().getPluginManager().registerCommand(plugin, new InternalBungeeCommand(this, command));
    }

    @Override
    public void unregisterCommand(String name) {
        Command<BungeeSource> imperatCmd = getCommand(name);
        super.unregisterCommand(name);
        if (imperatCmd == null) return;

        for (var entry : new HashSet<>(plugin.getProxy().getPluginManager().getCommands())) {
            var key = StringUtils.stripNamespace(entry.getKey());

            if (imperatCmd.hasName(key)) {
                plugin.getProxy().getPluginManager().unregisterCommand(entry.getValue());
            }
        }
    }

    @Override
    public BungeeSource wrapSender(Object sender) {
        return new BungeeSource(adventureProvider, (CommandSender) sender);
    }

    @Override
    public Plugin getPlatform() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        this.adventureProvider.close();
        this.plugin.onDisable();
    }
}
