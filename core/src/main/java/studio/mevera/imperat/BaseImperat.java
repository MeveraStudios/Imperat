package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.AnnotationReader;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandRegistry;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventListenerConsumer;
import studio.mevera.imperat.events.ExecutionStrategy;
import studio.mevera.imperat.events.types.CommandPostRegistrationEvent;
import studio.mevera.imperat.events.types.CommandPreRegistrationEvent;
import studio.mevera.imperat.exception.AmbiguousCommandException;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class BaseImperat<S extends CommandSource> implements Imperat<S> {

    protected final ImperatConfig<S> config;
    private final CommandRegistry<S> commandRegistry;
    private final ImperatExecutor<S> executor;
    private final ImperatAutoCompleter<S> autoCompleter;

    /**
     * Lazily initialized — built on first call that needs annotation parsing.
     * Programmatic-only embedders (no annotated command classes, no
     * {@code @ExceptionHandler}-annotated objects, no replacers) never
     * trigger construction. {@code volatile} for safe double-checked init.
     */
    private volatile @Nullable AnnotationParser<S> annotationParser;

    protected BaseImperat(@NotNull ImperatConfig<S> config) {
        this(config, new MapCommandRegistry<>());
    }

    /**
     * Constructor accepting a custom {@link CommandRegistry} — useful for
     * embedders needing persistence, distributed lookups, or audit logging on
     * the storage path. The default constructor wires {@link MapCommandRegistry}.
     */
    protected BaseImperat(@NotNull ImperatConfig<S> config, @NotNull CommandRegistry<S> commandRegistry) {
        this.config = config;
        this.commandRegistry = commandRegistry;
        this.executor = new ImperatExecutor<>(this, config);
        this.autoCompleter = new ImperatAutoCompleter<>(this, config);

        if (config.getEventBus().isDummyBus()) {
            config.setEventBus(DefaultEventBusFactory.create(this, config));
        }
        new ImperatEventBootstrap<>(this, config).registerDefaultListeners();
    }

    /**
     * Returns the (possibly-just-built) annotation parser. The first caller
     * triggers construction and replays any annotation replacers staged on the
     * config; subsequent calls return the cached instance.
     *
     * <p>Holding off until first use means a fully programmatic embedder —
     * one that registers via {@link #registerSimpleCommand} only — never pays
     * the parser-construction cost.</p>
     */
    private @NotNull AnnotationParser<S> getOrInitParser() {
        AnnotationParser<S> existing = annotationParser;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (annotationParser == null) {
                annotationParser = AnnotationParser.defaultParser(this);
                // Replay replacers staged on the config now that the parser exists.
                // ImperatConfig is sealed permits ImperatConfigImpl; cast is safe.
                ((ImperatConfigImpl<S>) config).installAnnotationReplacersInto(this);
            }
            return annotationParser;
        }
    }

    @Override
    public <E extends Event> void listen(
            @NotNull Class<E> eventType,
            @NotNull EventListenerConsumer<E> handler,
            @NotNull Priority priority,
            @NotNull ExecutionStrategy strategy
    ) {
        config.getEventBus().register(eventType, handler, priority, strategy);
    }

    @Override
    public <E extends Event> void publishEvent(E event) {
        config.getEventBus().post(event);
    }

    @Override
    public boolean removeListener(@NotNull UUID subscriptionId) {
        return config.getEventBus().unregister(subscriptionId);
    }

    @Override
    public @NotNull ImperatConfig<S> config() {
        return config;
    }

    @Override
    public boolean canBeSender(Type type) {
        return TypeWrap.of(CommandSource.class).isSupertypeOf(type);
    }

    /**
     * Registering a command into the global registry,
     * it will check for ambiguity with other commands and their tree before registering,
     * if an ambiguity is detected it will throw an {@link AmbiguousCommandException}
     *
     * @param command the command to register
     */
    @Override
    public void registerSimpleCommand(Command<S> command) {
        AmbiguityChecker.checkAmbiguity(command);
        CommandPreRegistrationEvent<S> preRegistrationEvent = new CommandPreRegistrationEvent<>(command);
        publishEvent(preRegistrationEvent);

        if (!preRegistrationEvent.isCancelled()) {
            Throwable error = null;
            try {
                commandRegistry.register(command);
            } catch (Throwable ex) {
                error = ex;
            } finally {
                CommandPostRegistrationEvent<S> postRegistrationEvent = new CommandPostRegistrationEvent<>(command, error);
                publishEvent(postRegistrationEvent);
            }
        } else {
            ImperatDebugger.debug("Registration of command '%s' was cancelled by an CommandPreRegistrationEvent.", command.getName());
        }
    }

    /**
     * Registers a command class built by the
     * annotations using a parser
     *
     * @param commandClass the annotated command instance to parse
     */
    @Override
    public void registerCommand(Class<?> commandClass) {
        Preconditions.notNull(commandClass, "commandClass");
        Object classInstance = config.getInstanceFactory().createInstance(config, commandClass);
        getOrInitParser().parseCommandClass(Objects.requireNonNull(classInstance));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerCommand(Object commandInstance) {
        if (commandInstance instanceof Command<?> command) {
            registerSimpleCommand((Command<S>) command);
        } else {
            getOrInitParser().parseCommandClass(Objects.requireNonNull(commandInstance));
        }
    }

    @Override
    public void unregisterCommand(String name) {
        commandRegistry.unregister(name);
    }

    @Override
    public void unregisterAllCommands() {
        commandRegistry.clear();
    }

    @Override
    public @Nullable Command<S> getCommand(final String name) {
        return commandRegistry.get(name);
    }

    /**
     * Registers a valueType of annotations so that it can be
     * detected by {@link AnnotationReader} , it's useful as it allows that valueType of annotation
     * to be recognized as a true Imperat-related annotation to be used in something like checking if a
     * {@link Argument} is annotated and checks for the annotations it has.
     *
     * @param type the valueType of annotation
     */
    @SafeVarargs
    @Override
    public final void registerAnnotations(Class<? extends Annotation>... type) {
        getOrInitParser().registerAnnotations(type);
    }

    @Override
    public <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        getOrInitParser().registerAnnotationReplacer(type, replacer);
    }

    @Override
    public @Nullable Command<S> getSubCommand(String owningCommand, String name) {
        Command<S> owningCmd = getCommand(owningCommand);
        if (owningCmd == null) {
            return null;
        }

        for (Command<S> subCommand : owningCmd.getSubCommands()) {
            Command<S> result = search(subCommand, name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Command<S> search(Command<S> sub, String name) {
        if (sub.hasName(name)) {
            return sub;
        }

        for (Command<S> other : sub.getSubCommands()) {
            if (other.hasName(name)) {
                return other;
            }
            return search(other, name);
        }
        return null;
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull CommandContext<S> context) {
        return executor.execute(context);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull Command<S> command, @NotNull String commandName, String[] rawInput) {
        return executor.execute(source, command, commandName, rawInput);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull String commandName, String[] rawInput) {
        return executor.execute(source, commandName, rawInput);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String rawArgsOneLine) {
        return executor.execute(sender, commandName, rawArgsOneLine);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String line) {
        return executor.execute(sender, line);
    }

    @Override
    public CompletableFuture<List<String>> autoComplete(@NotNull S source, @NotNull String fullCommandLine) {
        return autoCompleter.autoComplete(source, fullCommandLine);
    }

    @Override
    public Collection<? extends Command<S>> getRegisteredCommands() {
        return commandRegistry.values();
    }

    @Override
    public @NotNull AnnotationParser<S> getAnnotationParser() {
        return getOrInitParser();
    }

    @Override
    public void setAnnotationParser(AnnotationParser<S> parser) {
        Preconditions.notNull(parser, "Parser");
        synchronized (this) {
            this.annotationParser = parser;
        }
    }

    @Override
    public void debug() {
        for (var cmd : commandRegistry.values()) {
            cmd.visualizeTree();
        }
    }
}
