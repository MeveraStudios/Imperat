package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.providers.CommandSourceMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class JdaImperat<S extends JdaCommandSource> extends BaseImperat<S> {

    private final JDA jda;
    @SuppressWarnings("rawtypes")
    private final JdaSlashCommandListener listener;
    @SuppressWarnings("rawtypes")
    private final SlashCommandMapper slashCommandMapper = new SlashCommandMapper();
    @SuppressWarnings("rawtypes")
    private final Map<String, SlashCommandMapper.SlashMapping> slashMappings = new ConcurrentHashMap<>();
    private final AtomicBoolean syncScheduled = new AtomicBoolean(false);

    @SuppressWarnings({"rawtypes", "unchecked"}) JdaImperat(@NotNull JDA jda, @NotNull ImperatConfig<S> config) {
        super(config);
        this.jda = jda;
        this.listener = new JdaSlashCommandListener(this);
        this.jda.addEventListener(listener);
    }

    public static JdaConfigBuilder<JdaCommandSource> builder(@NotNull JDA jda) {
        return new JdaConfigBuilder<>(jda, JdaCommandSource.class, CommandSourceMapper.identity());
    }

    public static <S extends JdaCommandSource> JdaConfigBuilder<S> builder(
            @NotNull JDA jda, Class<S> sourceClass, CommandSourceMapper<JdaCommandSource, S> mapper
    ) {
        return new JdaConfigBuilder<>(jda, sourceClass, mapper);
    }

    @Override
    public void registerSimpleCommand(Command<S> command) {
        super.registerSimpleCommand(command);
        scheduleSync();
    }

    @SafeVarargs
    @Override
    public final void registerCommands(Command<S>... commands) {
        for (final var command : commands) {
            super.registerSimpleCommand(command);
        }
        scheduleSync();
    }

    @Override
    public void registerCommands(Class<?>... commands) {
        for (final var command : commands) {
            this.registerCommand(command);
        }
        scheduleSync();
    }

    @Override
    public void registerCommands(Object... commandInstances) {
        for (var obj : commandInstances) {
            super.registerCommand(obj);
        }
        scheduleSync();
    }

    @Override
    public void unregisterCommand(String name) {
        super.unregisterCommand(name);
        scheduleSync();
    }

    @Override
    public JDA getPlatform() {
        return jda;
    }

    @Override
    public void shutdownPlatform() {
        jda.removeEventListener(listener);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        JdaCommandSource platform = new JdaCommandSource(null);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        JdaCommandSource platform = new JdaCommandSource((SlashCommandInteractionEvent) sender);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @SuppressWarnings("rawtypes")
    SlashCommandMapper.SlashMapping getSlashMapping(String name) {
        return slashMappings.get(name.toLowerCase());
    }

    private void scheduleSync() {
        if (syncScheduled.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    syncCommands();
                } finally {
                    syncScheduled.set(false);
                }
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void syncCommands() {
        List<SlashCommandMapper.SlashMapping> mappings = new java.util.ArrayList<>();
        for (Object cmd : getRegisteredCommands()) {
            mappings.add(slashCommandMapper.mapCommand((Command) cmd));
        }

        slashMappings.clear();
        mappings.forEach(mapping -> slashMappings.put(mapping.commandName(), mapping));

        List<CommandData> data = mappings.stream()
                                         .map(m -> m.commandData())
                                         .collect(Collectors.toList());
        jda.updateCommands().addCommands(data).queue();
    }
}
