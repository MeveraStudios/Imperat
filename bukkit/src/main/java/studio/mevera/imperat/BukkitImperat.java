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

public final class BukkitImperat extends BaseImperat<BukkitSource> {

    private final Plugin plugin;
    private final boolean paperPlugin;
    private final AdventureProvider<CommandSender> adventureProvider;
    private BukkitBrigadierManager brigadierManager;
    private Map<String, org.bukkit.command.Command> bukkitCommands = new HashMap<>();

    public static BukkitConfigBuilder builder(Plugin plugin) {
        return new BukkitConfigBuilder(plugin);
    }

    @SuppressWarnings("unchecked")
    BukkitImperat(
            Plugin plugin,
            AdventureProvider<CommandSender> adventureProvider,
            boolean supportBrigadier,
            boolean injectCustomHelp,
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
        }

        //registering automatic help topic:
        if (injectCustomHelp) {
            Bukkit.getHelpMap().registerHelpTopicFactory(InternalBukkitCommand.class, new ImperatBukkitHelpTopic.Factory(this));
        }
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
    public void registerCommand(Command<BukkitSource> command) {
        super.registerCommand(command);

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

        if (imperatCmd == null) return;
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
        if (Version.isOrOver(13)) {
            brigadierManager = BukkitBrigadierManager.load(this);
        }
    }

    private boolean isPaperPlugin(Plugin plugin) {
        if (!Version.IS_PAPER || Version.isOrBelow(13)) {
            return false;
        }

        try {
            URI uri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();

            try (JarFile jar = new JarFile(new File(uri))) {
                return jar.getEntry("paper-plugin.yml") != null;
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

}
