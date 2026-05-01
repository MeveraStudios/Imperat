package studio.mevera.imperat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.providers.CommandSourceMapper;

import java.util.List;

public final class MinestomImperat<S extends MinestomCommandSource> extends BaseImperat<S> {

    private final ServerProcess serverProcess;

    MinestomImperat(@NotNull ServerProcess serverProcess, @NotNull ImperatConfig<S> config) {
        super(config);
        this.serverProcess = serverProcess;
        SyntaxDataLoader.migrateToImperatTypeData(this);
    }

    public static MinestomConfigBuilder<MinestomCommandSource> builder(@NotNull ServerProcess serverProcess) {
        return new MinestomConfigBuilder<>(serverProcess, MinestomCommandSource.class, CommandSourceMapper.identity());
    }

    public static <S extends MinestomCommandSource> MinestomConfigBuilder<S> builder(
            @NotNull ServerProcess serverProcess, Class<S> sourceClass, CommandSourceMapper<MinestomCommandSource, S> mapper
    ) {
        return new MinestomConfigBuilder<>(serverProcess, sourceClass, mapper);
    }

    @Override
    public ServerProcess getPlatform() {
        return serverProcess;
    }

    @Override
    public void shutdownPlatform() {
        serverProcess.stop();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        MinestomCommandSource platform = new MinestomCommandSource(MinecraftServer.getCommandManager().getConsoleSender());
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        if (!(sender instanceof CommandSender commandSender)) {
            throw new IllegalArgumentException("platform sender is not of valueType `" + CommandSender.class.getName() + "`");
        }
        MinestomCommandSource platform = new MinestomCommandSource(commandSender);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    public void registerSimpleCommand(Command<S> command) {
        super.registerSimpleCommand(command);
        MinecraftServer.getCommandManager().register(new InternalMinestomCommand<>(this, command));
    }

    @Override
    public void unregisterCommand(String name) {
        super.unregisterCommand(name);
        MinecraftServer.getCommandManager().getCommands().stream()
                .filter(cmd -> cmd.getName().equalsIgnoreCase(name) || List.of(cmd.getAliases()).contains(name.toLowerCase()))
                .forEach(MinecraftServer.getCommandManager()::unregister);
    }
}
