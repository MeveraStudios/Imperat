package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Imperat implementation for Discord using JDA slash commands.
 */
public final class JdaImperat extends BaseImperat<JdaSource> {

    private final JDA jda;
    private final JdaSlashCommandListener listener;
    private final SlashCommandMapper slashCommandMapper = new SlashCommandMapper();
    private final Map<String, SlashCommandMapper.SlashMapping> slashMappings = new ConcurrentHashMap<>();
    private final AtomicBoolean syncScheduled = new AtomicBoolean(false);

    JdaImperat(@NotNull JDA jda, @NotNull ImperatConfig<JdaSource> config) {
        super(config);
        this.jda = jda;
        this.listener = new JdaSlashCommandListener(this);
        this.jda.addEventListener(listener);
    }

    public static JdaConfigBuilder builder(@NotNull JDA jda) {
        return new JdaConfigBuilder(jda);
    }

    @Override
    public void registerSimpleCommand(Command<JdaSource> command) {
        super.registerSimpleCommand(command);
        scheduleSync();
    }

    @SafeVarargs
    @Override
    public final void registerCommands(Command<JdaSource>... commands) {
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
    public JdaSource wrapSender(Object sender) {
        return new JdaSource((SlashCommandInteractionEvent) sender);
    }

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

    private void syncCommands() {
        List<SlashCommandMapper.SlashMapping> mappings = getRegisteredCommands().stream()
                                                                 .map(slashCommandMapper::mapCommand)
                                                                 .toList();

        slashMappings.clear();
        mappings.forEach(mapping -> slashMappings.put(mapping.commandName(), mapping));

        List<CommandData> data = mappings.stream()
                                         .map(SlashCommandMapper.SlashMapping::commandData)
                                         .collect(Collectors.toList());
        jda.updateCommands().addCommands(data).queue();
    }
}