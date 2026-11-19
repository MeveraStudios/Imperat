package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.providers.CommandSourceMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JdaImperat<S extends JdaCommandSource> extends BaseImperat<S> {

    private final JDA jda;
    private final JdaSlashCommandListener listener;
    private final @Nullable JdaTextCommands textCommands;
    private final List<EventListener> textListeners = new ArrayList<>();
    private final SlashCommandMapper slashCommandMapper = new SlashCommandMapper();
    private final Map<String, SlashCommandMapper.SlashMapping> slashMappings = new ConcurrentHashMap<>();
    private final AtomicBoolean syncRequested = new AtomicBoolean(false);
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    JdaImperat(@NotNull JDA jda, @NotNull ImperatConfig<S> config, @Nullable JdaTextCommands textCommands) {
        super(config);
        this.jda = jda;
        this.textCommands = textCommands;
        this.listener = new JdaSlashCommandListener(this);
        this.jda.addEventListener(listener);
        if (textCommands != null) {
            textListeners.add(new JdaTextCommandListener(this));
            textListeners.add(JdaRevealReplier.listener());
            if (textCommands.privateReplier() instanceof EventListener own) {
                textListeners.add(own);
            }
            textListeners.forEach(this.jda::addEventListener);
        }
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
    public void unregisterAllCommands() {
        super.unregisterAllCommands();
        scheduleSync();
    }

    @Override
    public JDA getPlatform() {
        return jda;
    }

    @Override
    public void shutdownPlatform() {
        jda.removeEventListener(listener);
        textListeners.forEach(jda::removeEventListener);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        JdaCommandSource platform = JdaCommandSource.dummy();
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    /**
     * Wraps a {@link SlashCommandInteractionEvent} or an existing {@link JdaCommandSource}.
     * Text command sources are made by {@link #dispatchText}, which knows the trigger and path.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        JdaCommandSource platform;
        if (sender instanceof JdaCommandSource source) {
            platform = source;
        } else if (sender instanceof SlashCommandInteractionEvent event) {
            platform = JdaCommandSource.ofSlash(event);
        } else {
            throw new IllegalArgumentException("Cannot make a command source from " + sender);
        }
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    /** @return the text command settings, or {@code null} when text commands are off */
    public @Nullable JdaTextCommands getTextCommands() {
        return textCommands;
    }

    /**
     * Runs the text command in {@code event}, if it is one. The registered listener calls this for
     * every received message; call it yourself to feed messages from elsewhere.
     *
     * @return whether the message ran (or tried to run) a command
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean dispatchText(@NotNull MessageReceivedEvent event) {
        JdaTextCommands text = textCommands;
        if (text == null || !text.accepts(event)) {
            return false;
        }
        JdaTextInvocation.Typed typed = JdaTextInvocation.read(
                event.getMessage().getContentRaw(),
                text.prefixes(event),
                text.mentionPrefix() ? event.getJDA().getSelfUser().getId() : null
        );
        if (typed == null) {
            return false;
        }

        String path = text.triggers().resolve(event, typed.trigger());
        if (path == null && text.commandNames()) {
            Command<S> named = getCommand(typed.trigger());
            path = named == null ? null : named.getName();
        }
        if (path == null) {
            return false;
        }
        List<String> names = JdaTextInvocation.pathNames(path);
        if (names.isEmpty()) {
            return false;
        }
        Command<S> root = getCommand(names.get(0));
        if (root == null) {
            return false;
        }

        JdaTextInvocation.Resolved resolved = JdaTextInvocation.resolve(
                (Command<JdaCommandSource>) (Command) root, names, typed.remainder());
        S source = wrapSender(JdaCommandSource.ofMessage(event, typed.trigger(), resolved.commandPath(), text.privateReplier()));
        // Permissions are checked while executing; a refusal goes to the PermissionDeniedException handler.
        if (text.typing()) {
            event.getChannel().sendTyping().queue(null, ignored -> { });
        }
        execute(source, root.getName(), new String[]{resolved.input()});
        return true;
    }

    SlashCommandMapper.SlashMapping getSlashMapping(String name) {
        return slashMappings.get(name.toLowerCase());
    }

    private void scheduleSync() {
        syncRequested.set(true);

        if (syncRunning.compareAndSet(false, true)) {
            startSyncWorker();
        }
    }

    private void startSyncWorker() {
        CompletableFuture.runAsync(this::drainSync)
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        System.err.println("Failed to synchronize Discord commands: "
                                                   + error.getMessage());
                    }
                });
    }

    private void drainSync() {
        try {
            while (syncRequested.getAndSet(false)) {
                syncCommands().join();
            }
        } finally {
            syncRunning.set(false);

            if (syncRequested.get() && syncRunning.compareAndSet(false, true)) {
                startSyncWorker();
            }
        }
    }

    private CompletableFuture<Void> syncCommands() {
        slashMappings.clear();
        List<SlashCommandMapper.SlashMapping> mappings = new ArrayList<>();
        for (Command<S> cmd : getRegisteredCommands()) {
            SlashCommandMapper.SlashMapping mapping =
                    slashCommandMapper.mapCommand((Command<JdaCommandSource>) cmd);
            slashMappings.put(cmd.getName(), mapping);
            mappings.add(mapping);
        }

        List<? extends CommandData> data = mappings.stream()
                                                   .map(SlashCommandMapper.SlashMapping::commandData)
                                                   .toList();
        return jda.updateCommands()
                       .addCommands(data)
                       .submit()
                       .thenApply(ignored -> null);
    }

    /**
     * Synchronizes the registered commands to a guild.
     *
     * @param guild the guild whose commands should be replaced
     * @return a future completed when Discord has applied the command update
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<List<net.dv8tion.jda.api.interactions.commands.Command>> syncGuild(
            Guild guild
    ) {
        List<CommandData> data = new ArrayList<>();
        for (Object command : getRegisteredCommands()) {
            data.add(slashCommandMapper.mapCommand((Command) command).commandData());
        }

        return guild.updateCommands()
                       .addCommands(data)
                       .submit();
    }
}
