package studio.mevera.imperat;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.providers.CommandSourceMapper;

import java.util.ArrayList;
import java.util.List;

public class HytaleImperat<S extends HytaleCommandSource> extends BaseImperat<S> {

    protected final JavaPlugin plugin;

    HytaleImperat(JavaPlugin plugin, @NotNull ImperatConfig<S> config) {
        super(config);
        this.plugin = plugin;
    }

    public static HytaleConfigBuilder<HytaleCommandSource> builder(JavaPlugin plugin) {
        return new HytaleConfigBuilder<>(plugin, HytaleCommandSource.class, CommandSourceMapper.identity());
    }

    public static <S extends HytaleCommandSource> HytaleConfigBuilder<S> builder(
            JavaPlugin plugin, Class<S> sourceClass, CommandSourceMapper<HytaleCommandSource, S> mapper
    ) {
        return new HytaleConfigBuilder<>(plugin, sourceClass, mapper);
    }

    @Override
    public JavaPlugin getPlatform() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        HytaleServer.get().getPluginManager()
                .unload(plugin.getIdentifier());
    }

    @Override
    public void registerSimpleCommand(Command<S> command) {
        super.registerSimpleCommand(command);
        registerHytaleCommand(
                new InternalHytaleCommand<>(this, command)
        );
    }

    void registerHytaleCommand(InternalHytaleCommand<S> hytaleCommand) {
        plugin.getCommandRegistry().registerCommand(hytaleCommand);
    }

    @Override
    public void unregisterCommand(String name) {
        final Command<S> command = getCommand(name);
        if (command == null) {
            return;
        }

        List<String> aliases = new ArrayList<>(command.aliases());
        aliases.addFirst(name.toLowerCase());

        var commandManager = HytaleServer.get().getCommandManager();
        aliases.forEach(
                (alias) -> commandManager.getCommandRegistration().remove(alias.toLowerCase())
        );

        super.unregisterCommand(name);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        HytaleCommandSource platform = new HytaleCommandSource(ConsoleSender.INSTANCE);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        if (!(sender instanceof CommandSender cmdSender)) {
            throw new IllegalArgumentException("Sender object is not of type '" + CommandSender.class.getName() + "'");
        }
        HytaleCommandSource platform = new HytaleCommandSource(cmdSender);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }
}
