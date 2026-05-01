package studio.mevera.imperat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.StringUtils;

import java.util.HashSet;

public final class BungeeImperat<S extends BungeeCommandSource> extends BaseImperat<S> {

    private final Plugin plugin;
    private final AdventureProvider<CommandSender> adventureProvider;

    BungeeImperat(
            Plugin plugin,
            @NotNull AdventureProvider<CommandSender> adventureProvider,
            ImperatConfig<S> config
    ) {
        super(config);
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        ImperatDebugger.setLogger(plugin.getLogger());
    }

    public static BungeeConfigBuilder<BungeeCommandSource> builder(Plugin plugin) {
        return new BungeeConfigBuilder<>(plugin, BungeeCommandSource.class, CommandSourceMapper.identity(), null);
    }

    public static <S extends BungeeCommandSource> BungeeConfigBuilder<S> builder(
            Plugin plugin, Class<S> sourceClass, CommandSourceMapper<BungeeCommandSource, S> mapper
    ) {
        return new BungeeConfigBuilder<>(plugin, sourceClass, mapper, null);
    }

    @Override
    public void registerSimpleCommand(Command<S> command) {
        super.registerSimpleCommand(command);
        plugin.getProxy().getPluginManager().registerCommand(plugin, new InternalBungeeCommand<>(this, command));
    }

    @Override
    public void unregisterCommand(String name) {
        Command<S> imperatCmd = getCommand(name);
        super.unregisterCommand(name);
        if (imperatCmd == null) {
            return;
        }

        for (var entry : new HashSet<>(plugin.getProxy().getPluginManager().getCommands())) {
            var key = StringUtils.stripNamespace(entry.getKey());

            if (imperatCmd.hasName(key)) {
                plugin.getProxy().getPluginManager().unregisterCommand(entry.getValue());
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        BungeeCommandSource platform = new BungeeCommandSource(adventureProvider, plugin.getProxy().getConsole());
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        BungeeCommandSource platform = new BungeeCommandSource(adventureProvider, (CommandSender) sender);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
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
